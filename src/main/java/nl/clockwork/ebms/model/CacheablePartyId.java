package nl.clockwork.ebms.model;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import nl.clockwork.ebms.util.CPAUtils;

import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId;

public class CacheablePartyId implements List<PartyId>
{
	private List<PartyId> list;
	
	public CacheablePartyId(List<PartyId> list)
	{
		this.list = list;
	}

	@Override
	public int size()
	{
		return list.size();
	}

	@Override
	public boolean isEmpty()
	{
		return list.isEmpty();
	}

	@Override
	public boolean contains(Object o)
	{
		return list.contains(o);
	}

	@Override
	public Iterator<PartyId> iterator()
	{
		return list.iterator();
	}

	@Override
	public Object[] toArray()
	{
		return list.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a)
	{
		return (T[])list.toArray(a);
	}

	@Override
	public boolean add(PartyId e)
	{
		return list.add(e);
	}

	@Override
	public boolean remove(Object o)
	{
		return list.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c)
	{
		return list.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends PartyId> c)
	{
		return list.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends PartyId> c)
	{
		return list.addAll(index,c);
	}

	@Override
	public boolean removeAll(Collection<?> c)
	{
		return list.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c)
	{
		return list.retainAll(c);
	}

	@Override
	public void clear()
	{
		list.clear();
	}

	@Override
	public PartyId get(int index)
	{
		return list.get(index);
	}

	@Override
	public PartyId set(int index, PartyId element)
	{
		return list.set(index,element);
	}

	@Override
	public void add(int index, PartyId element)
	{
		list.add(index,element);
	}

	@Override
	public PartyId remove(int index)
	{
		return list.remove(index);
	}

	@Override
	public int indexOf(Object o)
	{
		return list.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o)
	{
		return list.lastIndexOf(o);
	}

	@Override
	public ListIterator<PartyId> listIterator()
	{
		return list.listIterator();
	}

	@Override
	public ListIterator<PartyId> listIterator(int index)
	{
		return list.listIterator(index);
	}

	@Override
	public List<PartyId> subList(int fromIndex, int toIndex)
	{
		return list.subList(fromIndex,toIndex);
	}

	@Override
	public String toString()
	{
		StringBuffer result = new StringBuffer();
		for (PartyId partyId : this)
			result.append(CPAUtils.toString((PartyId)partyId)).append(",");
		result.setLength(result.length() > 0 ? result.length() - 1 : result.length());
		return result.toString();
	}
}
