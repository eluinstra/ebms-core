package nl.clockwork.ebms;

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import io.vavr.control.Either;
import lombok.val;

public class Streams
{
	private static class EitherSpliterator<E,T> extends AbstractSpliterator<Either<E,T>>
	{
		private Spliterator<Either<E,T>> spliterator;
		private Predicate<Either<E,T>> predicate;
		private boolean stopCondition = false;

		public EitherSpliterator(Spliterator<Either<E,T>> spliterator, Predicate<Either<E,T>> predicate)
		{
			super(spliterator.estimateSize(),0);
			this.spliterator = spliterator;
			this.predicate = predicate;
		}

		@Override
		public synchronized boolean tryAdvance(Consumer<? super Either<E,T>> consumer)
		{
			boolean hadNext = spliterator.tryAdvance(either ->
			{
				if (!predicate.test(either))
					stopCondition = true;
				consumer.accept(either);
			});
			return hadNext && !stopCondition;
		}
	}

	public static <E,T> Either<E,List<T>> reduce(Stream<Either<E,T>> stream)
	{
		val identity = Either.<E,List<T>>right(new ArrayList<T>());
		BiFunction<Either<E,List<T>>,Either<E,T>,Either<E,List<T>>> accumulator = (e,item) -> 
		{
			if (e.isLeft())
				return e;
			if (item.isLeft())
				return Either.<E,List<T>>left(item.getLeft());
			val list = e.get();
			list.add(item.get());
			return Either.<E,List<T>>right(list);
		};
		BinaryOperator<Either<E,List<T>>> combiner = (e1,e2) ->
		{
			if (e1.isLeft())
				return e1;
			if (e2.isLeft())
				return e2;
			val list = Stream.concat(e1.get().stream(),e2.get().stream()).collect(Collectors.toList());
			return Either.<E,List<T>>right(list);
		};
		//return stream.reduce(identity,accumulator,combiner);
		val spliterator = new EitherSpliterator<E,T>(stream.spliterator(),e -> e.isRight());
		return StreamSupport.stream(spliterator,false).reduce(identity,accumulator,combiner);
	}
}
