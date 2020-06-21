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

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.sql.SQLQueryFactory;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.querydsl.model.QCpa;

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
@RequiredArgsConstructor
public class CPADAOImpl implements CPADAO
{
	@NonNull
	SQLQueryFactory queryFactory;
	@NonNull
	QCpa table = QCpa.cpa1;

	@Override
	@Transactional(transactionManager = "dataSourceTransactionManager")
	@Cacheable(cacheNames = "CPA", keyGenerator = "ebMSKeyGenerator")
	public boolean existsCPA(String cpaId)
	{
		return queryFactory.select(table.cpaId.count())
				.from(table)
				.where(table.cpaId.eq(cpaId))
				.fetchOne() > 0;
	}
	
	@Override
	@Transactional(transactionManager = "dataSourceTransactionManager")
	@Cacheable(cacheNames = "CPA", keyGenerator = "ebMSKeyGenerator")
	public Optional<CollaborationProtocolAgreement> getCPA(String cpaId)
	{
		return Optional.ofNullable(queryFactory.select(table.cpa)
				.from(table)
				.where(table.cpaId.eq(cpaId))
				.fetchOne());
	}

	@Override
	@Transactional(transactionManager = "dataSourceTransactionManager")
	@Cacheable(cacheNames = "CPA", keyGenerator = "ebMSKeyGenerator")
	public List<String> getCPAIds()
	{
		return queryFactory.select(table.cpaId)
				.from(table)
				.orderBy(table.cpaId.asc())
				.fetch();
	}

	@Override
	@Transactional(transactionManager = "dataSourceTransactionManager")
	@CacheEvict(cacheNames = "CPA", allEntries = true)
	public long insertCPA(CollaborationProtocolAgreement cpa)
	{
		return queryFactory.insert(table)
				.set(table.cpaId,cpa.getCpaid())
				.set(table.cpa,cpa)
				.execute();
	}

	@Override
	@Transactional(transactionManager = "dataSourceTransactionManager")
	@CacheEvict(cacheNames = "CPA", allEntries = true)
	public long updateCPA(CollaborationProtocolAgreement cpa)
	{
		return queryFactory.update(table)
				.set(table.cpa,cpa)
				.where(table.cpaId.eq(cpa.getCpaid()))
				.execute();
	}

	@Override
	@Transactional(transactionManager = "dataSourceTransactionManager")
	@CacheEvict(cacheNames = "CPA", allEntries = true)
	public long deleteCPA(String cpaId)
	{
		return queryFactory.delete(table)
				.where(table.cpaId.eq(cpaId))
				.execute();
	}
}
