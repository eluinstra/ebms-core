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
package nl.clockwork.ebms.api.cpa.certificate;

import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@CacheConfig(cacheNames = {"CertificateMapping"})
class CertificateMappingRepository
{
	@NonNull
	JdbcTemplate jdbcTemplate;

	@CacheEvict(cacheNames = "CertificateMapping", allEntries = true)
	public void clearCache()
	{
		// do nothing
	}

	@Cacheable(cacheNames = "CertificateMapping", keyGenerator = "ebMSKeyGenerator")
	public boolean existsCertificateMapping(String id, String cpaId)
	{
		return cpaId == null
				? jdbcTemplate.queryForObject("select count(*) from certificate_mapping where id = ? and cpa_id is null", Integer.class, id) > 0
				: jdbcTemplate.queryForObject("select count(*) from certificate_mapping where id = ? and cpa_id = ?", Integer.class, id, cpaId) > 0;
	}

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

	@CacheEvict(cacheNames = "CertificateMapping", allEntries = true)
	public int deleteCertificateMapping(String id, String cpaId)
	{
		return cpaId == null
				? jdbcTemplate.update("delete from certificate_mapping where id = ? and cpa_id is null", id)
				: jdbcTemplate.update("delete from certificate_mapping where id = ? and cpa_id = ?", id, cpaId);
	}
}
