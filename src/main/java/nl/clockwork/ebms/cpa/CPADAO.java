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

public interface CPADAO
{
	boolean existsCPA(String cpaId);
	Optional<CollaborationProtocolAgreement> getCPA(String cpaId);
	List<String> getCPAIds();
	String insertCPA(CollaborationProtocolAgreement cpa);
	int updateCPA(CollaborationProtocolAgreement cpa);
	int deleteCPA(String cpaId);
}