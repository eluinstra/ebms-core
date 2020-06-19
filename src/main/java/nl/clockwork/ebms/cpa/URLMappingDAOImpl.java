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

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.sql.SQLQueryFactory;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.querydsl.model.QUrlMapping;
import nl.clockwork.ebms.service.cpa.url.URLMapping;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class URLMappingDAOImpl implements URLMappingDAO
{
	@NonNull
	JdbcTemplate jdbcTemplate;
	@NonNull
	SQLQueryFactory queryFactory;
	QUrlMapping table = QUrlMapping.urlMapping;

	@Override
	public boolean existsURLMapping(String source)
	{
		//"select count(*) from url_mapping where source = ?"
		val query = queryFactory.select(table.source.count())
				.from(table)
				.where(table.source.eq(source))
				.getSQL();
		return jdbcTemplate.queryForObject(
				query.getSQL(),
				Integer.class,
				query.getNullFriendlyBindings().toArray()) > 0;
	}

	@Override
	public Optional<String> getURLMapping(String source)
	{
		try
		{
			//"select destination from url_mapping where source = ?"
			val query = queryFactory.select(table.source)
					.from(table)
					.where(table.source.eq(source))
					.getSQL();
			return Optional.of(jdbcTemplate.queryForObject(
					query.getSQL(),
					String.class,
					query.getNullFriendlyBindings().toArray()));
		}
		catch(EmptyResultDataAccessException e)
		{
			return Optional.empty();
		}
	}

	@Override
	public List<URLMapping> getURLMappings()
	{
		//"select source, destination from url_mapping order by source asc"
		val query = queryFactory.select(table.source,table.destination)
				.from(table)
				.orderBy(table.source.asc())
				.getSQL();
		return jdbcTemplate.query(
				query.getSQL(),
				(rs,rowNum) ->
				{
					return new URLMapping(rs.getString("source"),rs.getString("destination"));
				},
				query.getNullFriendlyBindings().toArray());
	}

	@Override
	@Transactional(transactionManager = "dataSourceTransactionManager")
	public long insertURLMapping(URLMapping urlMapping)
	{
		return queryFactory.insert(table)
				.set(table.source,urlMapping.getSource())
				.set(table.destination,urlMapping.getDestination())
				.execute();
	}

	@Override
	@Transactional(transactionManager = "dataSourceTransactionManager")
	public long updateURLMapping(URLMapping urlMapping)
	{
		return queryFactory.update(table)
				.set(table.destination,urlMapping.getDestination())
				.where(table.source.eq(urlMapping.getSource()))
				.execute();
	}

	@Override
	@Transactional(transactionManager = "dataSourceTransactionManager")
	public long deleteURLMapping(String source)
	{
		return queryFactory.delete(table)
				.where(table.source.eq(source))
				.execute();
	}
}
