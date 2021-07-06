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

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.xml.sax.SAXException;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.jaxb.JAXBParser;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
class CPADAOImpl implements CPADAO
{
	JdbcTemplate jdbcTemplate;

	@Override
	@Cacheable(cacheNames = "CPA", keyGenerator = "ebMSKeyGenerator")
	public boolean existsCPA(String cpaId)
	{
		return jdbcTemplate.queryForObject("select count(*) from cpa where cpa_id = ?",Integer.class,cpaId) > 0;
	}
	
	@Override
	@Cacheable(cacheNames = "CPA", keyGenerator = "ebMSKeyGenerator")
	public Optional<CollaborationProtocolAgreement> getCPA(String cpaId)
	{
		try
		{
			val result = jdbcTemplate.queryForObject("select cpa from cpa where cpa_id = ?",String.class,cpaId);
			return Optional.of(JAXBParser.getInstance(CollaborationProtocolAgreement.class).handle(result));
		}
		catch(EmptyResultDataAccessException e)
		{
			return Optional.empty();
		}
		catch (JAXBException | SAXException | ParserConfigurationException e)
		{
			throw new DataRetrievalFailureException("",e);
		}
	}

	@Override
	@Cacheable(cacheNames = "CPA", keyGenerator = "ebMSKeyGenerator")
	public List<String> getCPAIds()
	{
		return jdbcTemplate.queryForList("select cpa_id from cpa order by cpa_id asc",String.class);
	}

	@Override
	@CacheEvict(cacheNames = "CPA", allEntries = true)
	public String insertCPA(CollaborationProtocolAgreement cpa)
	{
		try
		{
			jdbcTemplate.update(
				"insert into cpa (cpa_id,cpa) values (?,?)",
				cpa.getCpaid(),
				JAXBParser.getInstance(CollaborationProtocolAgreement.class).handle(cpa));
			return cpa.getCpaid();
		}
		catch (JAXBException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	@CacheEvict(cacheNames = "CPA", allEntries = true)
	public int updateCPA(CollaborationProtocolAgreement cpa)
	{
		try
		{
			return jdbcTemplate.update(
				"update cpa set cpa = ? where cpa_id = ?",
				JAXBParser.getInstance(CollaborationProtocolAgreement.class).handle(cpa),
				cpa.getCpaid());
		}
		catch (JAXBException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	@CacheEvict(cacheNames = "CPA", allEntries = true)
	public int deleteCPA(String cpaId)
	{
		return jdbcTemplate.update("delete from cpa where cpa_id = ?",cpaId);
	}
}
