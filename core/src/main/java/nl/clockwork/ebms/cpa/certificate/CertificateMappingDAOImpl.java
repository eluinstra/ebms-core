/*
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
package nl.clockwork.ebms.cpa.certificate;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@CacheConfig(cacheNames = {"CertificateMapping"})
class CertificateMappingDAOImpl implements CertificateMappingDAO
{
	private static class CertificateRowMapper implements RowMapper<Tuple2<X509Certificate, String>>
	{
		@Override
		public Tuple2<X509Certificate, String> mapRow(ResultSet rs, int rowNum) throws SQLException
		{
			try
			{
				CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");
				return Tuple.of((X509Certificate)certificateFactory.generateCertificate(rs.getBinaryStream("destination")), rs.getString("cpa_id"));
			}
			catch (CertificateException e)
			{
				throw new SQLException(e);
			}
		}
	}

	@NonNull
	JdbcTemplate jdbcTemplate;

	@Override
	@CacheEvict(cacheNames = "CertificateMapping", allEntries = true)
	public void clearCache()
	{
		// do nothing
	}

	@Override
	@Cacheable(cacheNames = "CertificateMapping", keyGenerator = "ebMSKeyGenerator")
	public boolean existsCertificateMapping(String id, String cpaId)
	{
		return cpaId == null
				? jdbcTemplate.queryForObject("select count(*) from certificate_mapping where id = ? and cpa_id is null", Integer.class, id) > 0
				: jdbcTemplate.queryForObject("select count(*) from certificate_mapping where id = ? and cpa_id = ?", Integer.class, id, cpaId) > 0;
	}

	@Override
	@Cacheable(cacheNames = "CertificateMapping", keyGenerator = "ebMSKeyGenerator")
	public Optional<X509Certificate> getCertificateMapping(String id, String cpaId, boolean getSpecific)
	{
		try
		{
			val result = cpaId == null
					? jdbcTemplate.query("select destination, cpa_id from certificate_mapping where id = ? and cpa_id is null", new CertificateRowMapper(), id)
					: jdbcTemplate.query(
							"select destination, cpa_id from certificate_mapping where id = ?" + (getSpecific ? " and cpa_id = ?" : " and (cpa_id = ? or cpa_id is null)"),
							new CertificateRowMapper(),
							id,
							cpaId);
			if (result.size() == 0)
				return Optional.empty();
			else if (result.size() == 1)
				return Optional.of(result.get(0)._1);
			else
				return result.stream().filter(r -> r._2 != null).findFirst().map(r -> r._1);
		}
		catch (EmptyResultDataAccessException e)
		{
			return Optional.empty();
		}
	}

	@Override
	@Cacheable(cacheNames = "CertificateMapping", keyGenerator = "ebMSKeyGenerator")
	public List<CertificateMapping> getCertificateMappings()
	{
		return jdbcTemplate.query("select source, destination, cpa_id from certificate_mapping", new RowMapper<CertificateMapping>()
		{
			@Override
			public CertificateMapping mapRow(ResultSet rs, int nr) throws SQLException
			{
				try
				{
					val certificateFactory = CertificateFactory.getInstance("X509");
					val source = (X509Certificate)certificateFactory.generateCertificate(rs.getBinaryStream("source"));
					val destination = (X509Certificate)certificateFactory.generateCertificate(rs.getBinaryStream("destination"));
					val cpaId = rs.getString("cpa_id");
					return new CertificateMapping(source, destination, cpaId);
				}
				catch (CertificateException e)
				{
					throw new SQLException(e);
				}
			}
		});
	}

	@Override
	@CacheEvict(cacheNames = "CertificateMapping", allEntries = true)
	public void insertCertificateMapping(CertificateMapping mapping)
	{
		try
		{
			jdbcTemplate.update(
					"insert into certificate_mapping (id,source,destination,cpa_id) values (?,?,?,?)",
					mapping.getId(),
					mapping.getSource().getEncoded(),
					mapping.getDestination().getEncoded(),
					mapping.getCpaId());
		}
		catch (CertificateEncodingException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	@CacheEvict(cacheNames = "CertificateMapping", allEntries = true)
	public int updateCertificateMapping(CertificateMapping mapping)
	{
		val cpaId = mapping.getCpaId();
		try
		{
			return cpaId == null
					? jdbcTemplate
							.update("update certificate_mapping set destination = ? where id = ? and cpa_id is null", mapping.getDestination().getEncoded(), mapping.getId())
					: jdbcTemplate.update(
							"update certificate_mapping set destination = ? where id = ? and cpa_id = ?",
							mapping.getDestination().getEncoded(),
							mapping.getId(),
							cpaId);
		}
		catch (CertificateEncodingException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	@CacheEvict(cacheNames = "CertificateMapping", allEntries = true)
	public int deleteCertificateMapping(String id, String cpaId)
	{
		return cpaId == null
				? jdbcTemplate.update("delete from certificate_mapping where id = ? and cpa_id is null", id)
				: jdbcTemplate.update("delete from certificate_mapping where id = ? and cpa_id = ?", id, cpaId);
	}
}
