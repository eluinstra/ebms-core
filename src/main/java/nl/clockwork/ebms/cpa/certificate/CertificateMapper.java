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

import java.security.cert.X509Certificate;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class CertificateMapper
{
	@NonNull
	CertificateMappingDAO certificateMappingDAO;
	Object certificateMonitor = new Object();

	public List<CertificateMapping> getCertificates()
	{
		return certificateMappingDAO.getCertificateMappings();
	}

	public X509Certificate getCertificate(X509Certificate certificate, String cpaId)
	{
		return certificate != null ? certificateMappingDAO.getCertificateMapping(CertificateMapping.getCertificateId(certificate),cpaId,false).orElse(certificate) : null;
	}

	public void setCertificateMapping(CertificateMapping mapping)
	{
		synchronized (certificateMonitor)
		{
			if (certificateMappingDAO.existsCertificateMapping(mapping.getId(),mapping.getCpaId()))
				certificateMappingDAO.updateCertificateMapping(mapping);
			else
				certificateMappingDAO.insertCertificateMapping(mapping);
		}
	}

	public int deleteCertificateMapping(X509Certificate source, String cpaId)
	{
		val key = CertificateMapping.getCertificateId(source);
		return certificateMappingDAO.deleteCertificateMapping(key,cpaId);
	}

	public void deleteCache()
	{
		certificateMappingDAO.clearCache();
	}
}
