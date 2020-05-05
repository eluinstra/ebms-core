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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import net.sf.ehcache.Ehcache;
import nl.clockwork.ebms.common.MethodCacheInterceptor;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.service.model.CertificateMapping;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class CertificateMapper
{
	@NonNull
	Ehcache daoMethodCache;
	@NonNull
	EbMSDAO ebMSDAO;

	public List<CertificateMapping> getCertificates()
	{
		return ebMSDAO.getCertificateMappings();
	}

	public X509Certificate getCertificate(X509Certificate certificate)
	{
		return certificate != null ? ebMSDAO.getCertificateMapping(getId(certificate)).orElse(certificate) : null;
	}

	public void setCertificateMapping(CertificateMapping mapping)
	{
		val id = getId(mapping.getSource());
		if (mapping.getDestination() == null)
			ebMSDAO.deleteCertificateMapping(id);
		else
		{
			if (ebMSDAO.existsCertificateMapping(id))
				ebMSDAO.updateCertificateMapping(id,mapping);
			else
				ebMSDAO.insertCertificateMapping(id,mapping);
		}
		flushDAOMethodCache(id);
	}

	public void deleteCertificateMapping(X509Certificate source)
	{
		val key = getId(source);
		ebMSDAO.deleteCertificateMapping(key);
		flushDAOMethodCache(key);
	}
	
	private String getId(X509Certificate certificate)
	{
		return certificate.getIssuerX500Principal().getName() + certificate.getSerialNumber().toString();
	}

	private void flushDAOMethodCache(String key)
	{
		daoMethodCache.remove(MethodCacheInterceptor.getCacheKey("EbMSDAOImpl","existsCertificateMapping",key));
		daoMethodCache.remove(MethodCacheInterceptor.getCacheKey("EbMSDAOImpl","getCertificateMapping",key));
		daoMethodCache.remove(MethodCacheInterceptor.getCacheKey("EbMSDAOImpl","getCertificateMappings"));
	}
}
