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
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import com.querydsl.sql.SQLQueryFactory;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.querydsl.model.QCertificateMapping;
import nl.clockwork.ebms.service.cpa.certificate.CertificateMapping;

@Transactional(transactionManager = "transactionManager")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class CertificateMappingDAOImpl implements CertificateMappingDAO
{
	@NonNull
	SQLQueryFactory queryFactory;
	@NonNull
	QCertificateMapping table = QCertificateMapping.certificateMapping;

	@Override
	public boolean existsCertificateMapping(String id, String cpaId) throws DAOException
	{
		return queryFactory.select(table.source.count())
				.from(table)
				.where(table.id.eq(id)
						.and(cpaId == null ? table.cpaId.isNull() : table.cpaId.eq(cpaId)))
				.fetchOne() > 0L;
	}

	@Override
	public Optional<X509Certificate> getCertificateMapping(String id, String cpaId) throws DAOException
	{
		val result = queryFactory.select(table.source)
				.from(table)
				.where(table.id.eq(id)
						.and(cpaId == null ? table.cpaId.isNull() : table.cpaId.eq(cpaId)))
				.fetchOne();
		return Optional.of(result);
	}

	@Override
	public List<CertificateMapping> getCertificateMappings() throws DAOException
	{
		val result = queryFactory.select(table.source,table.destination,table.cpaId)
				.from(table)
				.fetch();
		return result.stream()
				.map(t -> new CertificateMapping(t.get(table.source),t.get(table.destination),t.get(table.cpaId)))
				.collect(Collectors.toList());
	}

	@Override
	public void insertCertificateMapping(String id, CertificateMapping mapping) throws DAOException
	{
		queryFactory.insert(table)
				.set(table.id,id)
				.set(table.source,mapping.getSource())
				.set(table.destination,mapping.getDestination())
				.set(table.cpaId,mapping.getCpaId())
				.execute();
	}

	@Override
	public int updateCertificateMapping(String id, CertificateMapping mapping) throws DAOException
	{
		return (int)queryFactory.update(table)
				.set(table.destination,mapping.getDestination())
				.set(table.cpaId,mapping.getCpaId())
				.where(table.id.eq(id)
						.and(mapping.getCpaId() == null ? table.cpaId.isNull() : table.cpaId.eq(mapping.getCpaId())))
				.execute();
	}

	@Override
	public int deleteCertificateMapping(String id, String cpaId) throws DAOException
	{
		return (int)queryFactory.delete(table)
				.where(table.id.eq(id)
						.and(cpaId == null ? table.cpaId.isNull() : table.cpaId.eq(cpaId)))
				.execute();
	}

	@Override
	public String getTargetName()
	{
		return getClass().getSimpleName();
	}
}
