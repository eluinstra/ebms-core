/**
 * Copyright 2011 Clockwork
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.clockwork.ebms.cpa;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.sql.SQLQueryFactory;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.service.cpa.url.URLMapping;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
class URLMappingDAOImpl implements URLMappingDAO
{
	@NonNull
	SQLQueryFactory queryFactory;
	QUrlMapping table = QUrlMapping.urlMapping;
	ConstructorExpression<URLMapping> urlMappingProjection = Projections.constructor(URLMapping.class,table.source,table.destination);

	@Override
	@Cacheable(cacheNames = "URLMapping", keyGenerator = "ebMSKeyGenerator")
	public boolean existsURLMapping(String source)
	{
		return queryFactory.select(table.source.count())
				.from(table)
				.where(table.source.eq(source))
				.fetchOne() > 0;
	}

	@Override
	@Cacheable(cacheNames = "URLMapping", keyGenerator = "ebMSKeyGenerator")
	public Optional<String> getURLMapping(String source)
	{
		return Optional.ofNullable(queryFactory.select(table.destination)
				.from(table)
				.where(table.source.eq(source))
				.fetchOne());
	}

	@Override
	@Cacheable(cacheNames = "URLMapping", keyGenerator = "ebMSKeyGenerator")
	public List<URLMapping> getURLMappings()
	{
		return queryFactory.select(urlMappingProjection)
				.from(table)
				.orderBy(table.source.asc())
				.fetch();
	}

	@Override
	@CacheEvict(cacheNames = "URLMapping", allEntries = true)
	public long insertURLMapping(URLMapping urlMapping)
	{
		return queryFactory.insert(table)
				.set(table.source,urlMapping.getSource())
				.set(table.destination,urlMapping.getDestination())
				.execute();
	}

	@Override
	@CacheEvict(cacheNames = "URLMapping", allEntries = true)
	public long updateURLMapping(URLMapping urlMapping)
	{
		return queryFactory.update(table)
				.set(table.destination,urlMapping.getDestination())
				.where(table.source.eq(urlMapping.getSource()))
				.execute();
	}

	@Override
	@CacheEvict(cacheNames = "URLMapping", allEntries = true)
	public long deleteURLMapping(String source)
	{
		return queryFactory.delete(table)
				.where(table.source.eq(source))
				.execute();
	}
}
