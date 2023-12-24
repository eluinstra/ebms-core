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
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class CertificateMappingServiceImpl implements CertificateMappingService
{
	CertificateMapper certificateMapper;

	@Override
	public void setCertificateMapping(nl.clockwork.ebms.cpa.certificate.CertificateMapping certificateMapping) throws CertificateMappingServiceException
	{
		try
		{
			setCertificateMappingImpl(certificateMapping);
		}
		catch (CertificateMappingServiceException e)
		{
			log.error("SetCertificateMapping", e);
			throw e;
		}
		catch (Exception e)
		{
			log.error("SetCertificateMapping " + certificateMapping, e);
			throw new CertificateMappingServiceException(e);
		}
	}

	protected void setCertificateMappingImpl(nl.clockwork.ebms.cpa.certificate.CertificateMapping certificateMapping)
	{
		if (log.isDebugEnabled())
			log.debug("SetCertificateMapping " + certificateMapping);
		certificateMapper.setCertificateMapping(certificateMapping);
	}

	@Override
	public void deleteCertificateMapping(X509Certificate source, String cpaId) throws CertificateMappingServiceException
	{
		try
		{
			deleteCertificateMappingImpl(source, cpaId);
		}
		catch (CertificateMappingServiceException e)
		{
			log.error("DeleteCertificateMapping", e);
			throw e;
		}
		catch (Exception e)
		{
			log.error("DeleteCertificateMapping " + source, e);
			throw new CertificateMappingServiceException(e);
		}
	}

	protected void deleteCertificateMappingImpl(X509Certificate source, String cpaId)
	{
		if (log.isDebugEnabled())
			log.debug("DeleteCertificateMapping " + source);
		if (certificateMapper.deleteCertificateMapping(source, cpaId) == 0)
			throw new CertificateNotFoundException();
	}

	@Override
	public List<nl.clockwork.ebms.cpa.certificate.CertificateMapping> getCertificateMappings() throws CertificateMappingServiceException
	{
		try
		{
			return getCertificateMappingsImpl();
		}
		catch (CertificateMappingServiceException e)
		{
			log.error("GetCertificateMappings", e);
			throw e;
		}
		catch (Exception e)
		{
			log.error("GetCertificateMappings", e);
			throw new CertificateMappingServiceException(e);
		}
	}

	protected List<nl.clockwork.ebms.cpa.certificate.CertificateMapping> getCertificateMappingsImpl()
	{
		log.debug("GetCertificateMappings");
		return certificateMapper.getCertificates();
	}

	@Override
	public void deleteCache() throws CertificateMappingServiceException
	{
		try
		{
			deleteCacheImpl();
		}
		catch (Exception e)
		{
			log.error("DeleteCache", e);
			throw new CertificateMappingServiceException(e);
		}
	}

	protected void deleteCacheImpl()
	{
		certificateMapper.deleteCache();
	}
}
