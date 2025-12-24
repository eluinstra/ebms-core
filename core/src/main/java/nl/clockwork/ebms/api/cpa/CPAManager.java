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
package nl.clockwork.ebms.api.cpa;

import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
@CacheConfig(cacheNames = {"CPA"})
public class CPAManager
{
	@NonNull
	CPARepository cpaDAO;
	Object cpaMonitor = new Object();

	@CacheEvict(cacheNames = {"CPA", "CPAQuery"}, allEntries = true)
	public void clearCache()
	{
		// do nothing
	}

	@Cacheable(cacheNames = "CPA", keyGenerator = "ebMSKeyGenerator")
	public boolean existsCPA(String cpaId)
	{
		return cpaDAO.existsCPA(cpaId);
	}

	@Cacheable(cacheNames = "CPA", keyGenerator = "ebMSKeyGenerator")
	public Optional<CollaborationProtocolAgreement> getCPA(String cpaId)
	{
		return cpaDAO.getCPA(cpaId);
	}

	public List<String> getCPAIds()
	{
		return cpaDAO.getCPAIds();
	}

	public void setCPA(CollaborationProtocolAgreement cpa, Boolean overwrite)
	{
		synchronized (cpaMonitor)
		{
			if (cpaDAO.existsCPA(cpa.getCpaid()))
			{
				if (overwrite != null && overwrite)
				{
					if (cpaDAO.updateCPA(cpa) == 0)
						throw new IllegalArgumentException("Could not update CPA " + cpa.getCpaid() + "! CPA does not exists.");
				}
				else
					throw new IllegalArgumentException("Did not insert CPA " + cpa.getCpaid() + "! CPA already exists.");
			}
			else
				cpaDAO.insertCPA(cpa);
		}
	}

	public int deleteCPA(String cpaId)
	{
		return cpaDAO.deleteCPA(cpaId);
	}

}
