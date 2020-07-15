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

import nl.clockwork.ebms.service.cpa.certificate.CertificateMapping;

interface CertificateMappingDAO
{
	boolean existsCertificateMapping(String id, String cpaId);
	Optional<X509Certificate> getCertificateMapping(String id, String cpaId, boolean getSpecific);
	List<CertificateMapping> getCertificateMappings();
	void insertCertificateMapping(CertificateMapping mapping);
	int updateCertificateMapping(CertificateMapping mapping);
	int deleteCertificateMapping(String id, String cpaId);
}