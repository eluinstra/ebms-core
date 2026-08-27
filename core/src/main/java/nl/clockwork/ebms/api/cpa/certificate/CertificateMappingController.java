/*
 * Copyright 2011 - 2026 Clockwork
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

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.security.cert.X509Certificate;
import java.util.List;
import nl.clockwork.ebms.common.cpa.certificate.CertificateMapping;
import nl.clockwork.ebms.common.cpa.certificate.X509CertificateAdapter;

@WebService(
		name = "CertificateMappingService",
		targetNamespace = "http://www.ordina.nl/cpa/certificateMapping/2.18",
		serviceName = "CertificateMappingService",
		portName = "CertificateMappingPort")
public interface CertificateMappingController
{
	/**
	 * Stores Certificate mapping certificateMapping
	 * 
	 * @param certificateMapping - Maps the source Certificate to the destination Certificate
	 * @throws CertificateMappingControllerException
	 */
	@WebMethod(operationName = "setCertificateMapping")
	void setCertificateMapping(@WebParam(name = "certificateMapping") @XmlElement(required = true) CertificateMapping certificateMapping)
			throws CertificateMappingControllerException;

	/**
	 * Removes Certificate mapping identified by source Certificate source
	 * 
	 * @param source
	 * @throws CertificateMappingControllerException
	 */
	@WebMethod(operationName = "deleteCertificateMapping")
	void deleteCertificateMapping(
			@WebParam(name = "sourceCertificate") @XmlElement(required = true) @XmlJavaTypeAdapter(X509CertificateAdapter.class) X509Certificate source,
			@WebParam(name = "cpaId") String cpaId) throws CertificateMappingControllerException;

	/**
	 * Returns a list of all Certificate mappings
	 * 
	 * @return The list of Certificate mappings
	 * @throws CertificateMappingControllerException
	 */
	@WebResult(name = "certificate")
	@WebMethod(operationName = "getCertificateMappings")
	List<CertificateMapping> getCertificateMappings() throws CertificateMappingControllerException;

	/**
	 * Deletes the Certificate mapping cache
	 * 
	 * @throws CertificateMappingControllerException
	 */
	@WebMethod(operationName = "deleteCache")
	void deleteCache() throws CertificateMappingControllerException;
}
