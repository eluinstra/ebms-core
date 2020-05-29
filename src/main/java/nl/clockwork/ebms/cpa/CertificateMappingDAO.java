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

import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.service.cpa.certificate.CertificateMapping;

public interface CertificateMappingDAO
{
	boolean existsCertificateMapping(String id, String cpaId) throws DAOException;
	Optional<X509Certificate> getCertificateMapping(String id, String cpaId) throws DAOException;
	List<CertificateMapping> getCertificateMappings() throws DAOException;
	void insertCertificateMapping(String id, CertificateMapping mapping) throws DAOException;
	int updateCertificateMapping(String id, CertificateMapping mapping) throws DAOException;
	int deleteCertificateMapping(String id, String cpaId) throws DAOException;
	String getTargetName();
}