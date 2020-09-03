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

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.sql.SQLQueryFactory;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.service.cpa.certificate.CertificateMapping;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
class CertificateMappingDAOImpl implements CertificateMappingDAO
{
	@NonNull
	SQLQueryFactory queryFactory;
	@NonNull
	QCertificateMapping table = QCertificateMapping.certificateMapping;
	ConstructorExpression<CertificateMapping> certificateMappingProjection = Projections.constructor(CertificateMapping.class,table.source,table.destination,table.cpaId);

	@Override
	@Cacheable(cacheNames = "CertificateMapping", keyGenerator = "ebMSKeyGenerator")
	public boolean existsCertificateMapping(String id, String cpaId)
	{
		return queryFactory.select(table.source.count())
				.from(table)
				.where(table.id.eq(id)
						.and(cpaId == null ? table.cpaId.isNull() : table.cpaId.eq(cpaId)))
				.fetchOne() > 0;
	}

	@Override
	@Cacheable(cacheNames = "CertificateMapping", keyGenerator = "ebMSKeyGenerator")
	public Optional<X509Certificate> getCertificateMapping(String id, String cpaId, boolean getSpecific)
	{
		val result = queryFactory.select(table.destination,table.cpaId)
				.from(table)
				.where(table.id.eq(id)
						.and(cpaId == null ? table.cpaId.isNull() : getSpecific ? table.cpaId.eq(cpaId) : table.cpaId.eq(cpaId).or(table.cpaId.isNull())))
				.fetch();
		if (result.size() == 0)
			return Optional.empty();
		else if (result.size() == 1)
			return Optional.of(result.get(0).get(table.destination));
		else
			return result.stream().filter(t -> t.get(table.cpaId) != null).findFirst().map(t -> t.get(table.destination));
	}

	@Override
	@Cacheable(cacheNames = "CertificateMapping", keyGenerator = "ebMSKeyGenerator")
	public List<CertificateMapping> getCertificateMappings()
	{
		return queryFactory.select(certificateMappingProjection)
				.from(table)
				.fetch();
	}

	@Override
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
	@CacheEvict(cacheNames = "CertificateMapping", allEntries = true)
	public int deleteCertificateMapping(String id, String cpaId)
	{
		return (int)queryFactory.delete(table)
				.where(table.id.eq(id)
						.and(cpaId == null ? table.cpaId.isNull() : table.cpaId.eq(cpaId)))
				.execute();
	}
}
