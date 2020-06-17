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

import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.querydsl.sql.SQLQueryFactory;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.querydsl.model.QUrlMapping;
import nl.clockwork.ebms.service.cpa.url.URLMapping;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class URLMappingDAOImpl implements URLMappingDAO
{
	JdbcTemplate jdbcTemplate;
	SQLQueryFactory queryFactory;
	QUrlMapping table = QUrlMapping.urlMapping;

	@Override
	public boolean existsURLMapping(String source) throws DAOException
	{
		val query = queryFactory.select(table.source.count())
				.from(table)
				.where(table.source.eq(source))
				.getSQL();
		try
		{
			return jdbcTemplate.queryForObject(
				query.getSQL(),
				Integer.class,
				query.getNullFriendlyBindings().toArray()
			) > 0;
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public Optional<String> getURLMapping(String source) throws DAOException
	{
		val query = queryFactory.select(table.source)
				.from(table)
				.where(table.source.eq(source))
				.getSQL();
		try
		{
			return Optional.of(jdbcTemplate.queryForObject(
				query.getSQL(),
				String.class,
				query.getNullFriendlyBindings().toArray()
			));
		}
		catch(EmptyResultDataAccessException e)
		{
			return Optional.empty();
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public List<URLMapping> getURLMappings() throws DAOException
	{
		val query = queryFactory.select(table.source,table.destination)
				.from(table)
				.getSQL();
		try
		{
			return jdbcTemplate.query(
				query.getSQL(),
				(rs,rowNum) ->
				{
					return new URLMapping(rs.getString("source"),rs.getString("destination"));
				}
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public void insertURLMapping(URLMapping urlMapping) throws DAOException
	{
		val query = queryFactory.insert(table)
				.set(table.source,urlMapping.getSource())
				.set(table.destination,urlMapping.getDestination())
				.getSQL();
		try
		{
			jdbcTemplate.update
			(
				query.get(0).getSQL(),
				query.get(0).getNullFriendlyBindings().toArray()
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public int updateURLMapping(URLMapping urlMapping) throws DAOException
	{
		val query = queryFactory.update(table)
				.set(table.destination,urlMapping.getDestination())
				.where(table.source.eq(urlMapping.getSource()))
				.getSQL();
		try
		{
			return jdbcTemplate.update
			(
				query.get(0).getSQL(),
				query.get(0).getNullFriendlyBindings().toArray()
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public int deleteURLMapping(String source) throws DAOException
	{
		val query = queryFactory.delete(table)
				.where(table.source.eq(source))
				.getSQL();
		try
		{
			return jdbcTemplate.update
			(
				query.get(0).getSQL(),
				query.get(0).getNullFriendlyBindings().toArray()
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public final String getTargetName()
	{
		return getClass().getSimpleName();
	}
}
