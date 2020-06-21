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

import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.sql.SQLQueryFactory;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.querydsl.model.QCertificateMapping;
import nl.clockwork.ebms.service.cpa.certificate.CertificateMapping;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class CertificateMappingDAOImpl implements CertificateMappingDAO
{
	@NonNull
	JdbcTemplate jdbcTemplate;
	@NonNull
	SQLQueryFactory queryFactory;
	@NonNull
	QCertificateMapping table = QCertificateMapping.certificateMapping;

	@Override
	@Cacheable(cacheNames = "CertificateMapping", keyGenerator = "ebMSKeyGenerator")
	public boolean existsCertificateMapping(String id, String cpaId)
	{
		//"select count(*) from certificate_mapping where id = ? and (cpa_id = ? or (cpa_id is null and ? is null))"
		val query = queryFactory.select(table.source.count())
				.from(table)
				.where(table.id.eq(id)
						.and(cpaId == null ? table.cpaId.isNull() : table.cpaId.eq(cpaId)))
				.getSQL();
		return jdbcTemplate.queryForObject(
				query.getSQL(),
				query.getNullFriendlyBindings().toArray(),
				Integer.class) > 0;
	}

	@Override
	@Cacheable(cacheNames = "CertificateMapping", keyGenerator = "ebMSKeyGenerator")
	public Optional<X509Certificate> getCertificateMapping(String id, String cpaId)
	{
		try
		{
			//"select destination from certificate_mapping where id = ? and (cpa_id = ? or (cpa_id is null and ? is null))"
			val query = queryFactory.select(table.source,table.cpaId)
					.from(table)
					.where(table.id.eq(id)
							.and(cpaId == null ? table.cpaId.isNull() : table.cpaId.eq(cpaId).or(table.cpaId.isNull())))
					.getSQL();
			return jdbcTemplate.query(
					query.getSQL(),
					query.getNullFriendlyBindings().toArray(),
					(ResultSetExtractor<Optional<X509Certificate>>)(rs) ->
					{
						if (!rs.next())
							return Optional.empty();
						while (rs.getString("cpaId") == null && !rs.isLast())
							rs.next();
						try
						{
							CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");
							return Optional.of((X509Certificate)certificateFactory.generateCertificate(rs.getBinaryStream("source")));
						}
						catch (CertificateException e)
						{
							throw new SQLException(e);
						}
					});
		}
		catch(EmptyResultDataAccessException e)
		{
			return Optional.empty();
		}
	}

	@Override
	@Cacheable(cacheNames = "CertificateMapping", keyGenerator = "ebMSKeyGenerator")
	public List<CertificateMapping> getCertificateMappings()
	{
		//"select source, destination, cpa_id from certificate_mapping"
		val query = queryFactory.select(table.source,table.destination,table.cpaId)
				.from(table)
				.getSQL();
		return jdbcTemplate.query(
				query.getSQL(),
				query.getNullFriendlyBindings().toArray(),
				(rs,rowNum) ->
				{
					try
					{
						val certificateFactory = CertificateFactory.getInstance("X509");
						val source = (X509Certificate)certificateFactory.generateCertificate(rs.getBinaryStream("source"));
						val destination = (X509Certificate)certificateFactory.generateCertificate(rs.getBinaryStream("destination"));
						val cpaId = rs.getString("cpa_id");
						return new CertificateMapping(source,destination,cpaId);
					}
					catch (CertificateException e)
					{
						throw new SQLException(e);
					}
				});
	}

	@Override
	@Transactional(transactionManager = "dataSourceTransactionManager")
	@CacheEvict(cacheNames = "CertificateMapping", allEntries = true)
	public void insertCertificateMapping(CertificateMapping mapping)
	{
		queryFactory.insert(table)
				.set(table.id,mapping.getId())
				.set(table.source,mapping.getSource())
				.set(table.destination,mapping.getDestination())
				.set(table.cpaId,mapping.getCpaId())
				.execute();
	}

	@Override
	@Transactional(transactionManager = "dataSourceTransactionManager")
	@CacheEvict(cacheNames = "CertificateMapping", allEntries = true)
	public int updateCertificateMapping(CertificateMapping mapping)
	{
		return (int)queryFactory.update(table)
				.set(table.destination,mapping.getDestination())
				.set(table.cpaId,mapping.getCpaId())
				.where(table.id.eq(mapping.getId())
						.and(mapping.getCpaId() == null ? table.cpaId.isNull() : table.cpaId.eq(mapping.getCpaId())))
				.execute();
	}

	@Override
	@Transactional(transactionManager = "dataSourceTransactionManager")
	@CacheEvict(cacheNames = "CertificateMapping", allEntries = true)
	public int deleteCertificateMapping(String id, String cpaId)
	{
		return (int)queryFactory.delete(table)
				.where(table.id.eq(id)
						.and(cpaId == null ? table.cpaId.isNull() : table.cpaId.eq(cpaId)))
				.execute();
	}
}
