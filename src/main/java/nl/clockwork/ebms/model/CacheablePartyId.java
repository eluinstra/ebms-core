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
package nl.clockwork.ebms.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import nl.clockwork.ebms.util.CPAUtils;

import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId;

public class CacheablePartyId implements List<PartyId>
{
	private List<PartyId> partyIds;
	
	//TODO: needed for ordered messaging
	public CacheablePartyId(String partyId)
	{
		PartyId p = new PartyId();
		p.setValue(partyId);
		this.partyIds = Arrays.asList(p);
	}

	public CacheablePartyId(List<PartyId> partyIds)
	{
		this.partyIds = partyIds;
	}

	@Override
	public int size()
	{
		return partyIds.size();
	}

	@Override
	public boolean isEmpty()
	{
		return partyIds.isEmpty();
	}

	@Override
	public boolean contains(Object o)
	{
		return partyIds.contains(o);
	}

	@Override
	public Iterator<PartyId> iterator()
	{
		return partyIds.iterator();
	}

	@Override
	public Object[] toArray()
	{
		return partyIds.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a)
	{
		return (T[])partyIds.toArray(a);
	}

	@Override
	public boolean add(PartyId e)
	{
		return partyIds.add(e);
	}

	@Override
	public boolean remove(Object o)
	{
		return partyIds.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c)
	{
		return partyIds.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends PartyId> c)
	{
		return partyIds.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends PartyId> c)
	{
		return partyIds.addAll(index,c);
	}

	@Override
	public boolean removeAll(Collection<?> c)
	{
		return partyIds.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c)
	{
		return partyIds.retainAll(c);
	}

	@Override
	public void clear()
	{
		partyIds.clear();
	}

	@Override
	public PartyId get(int index)
	{
		return partyIds.get(index);
	}

	@Override
	public PartyId set(int index, PartyId element)
	{
		return partyIds.set(index,element);
	}

	@Override
	public void add(int index, PartyId element)
	{
		partyIds.add(index,element);
	}

	@Override
	public PartyId remove(int index)
	{
		return partyIds.remove(index);
	}

	@Override
	public int indexOf(Object o)
	{
		return partyIds.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o)
	{
		return partyIds.lastIndexOf(o);
	}

	@Override
	public ListIterator<PartyId> listIterator()
	{
		return partyIds.listIterator();
	}

	@Override
	public ListIterator<PartyId> listIterator(int index)
	{
		return partyIds.listIterator(index);
	}

	@Override
	public List<PartyId> subList(int fromIndex, int toIndex)
	{
		return partyIds.subList(fromIndex,toIndex);
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
