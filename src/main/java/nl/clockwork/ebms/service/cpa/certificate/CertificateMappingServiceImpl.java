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
package nl.clockwork.ebms.service.cpa.certificate;

import java.security.cert.X509Certificate;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.cpa.CertificateMapper;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class CertificateMappingServiceImpl implements CertificateMappingService
{
  @NonNull
	CertificateMapper certificateMapper;

	@Override
	public void setCertificateMapping(CertificateMapping certificateMapping) throws CertificateMappingServiceException
	{
		try
		{
			if (log.isDebugEnabled())
				log.debug("SetCertificateMapping " + certificateMapping);
			certificateMapper.setCertificateMapping(certificateMapping);
		}
		catch (Exception e)
		{
			log.error("SetCertificateMapping " + certificateMapping,e);
			throw new CertificateMappingServiceException(e);
		}
	}

	@Override
	public void deleteCertificateMapping(X509Certificate source, String cpaId) throws CertificateMappingServiceException
	{
		try
		{
			if (log.isDebugEnabled())
				log.debug("SetCertificateMapping" + source);
			certificateMapper.deleteCertificateMapping(source,cpaId);
		}
		catch (Exception e)
		{
			log.error("SetCertificateMapping" + source,e);
			throw new CertificateMappingServiceException(e);
		}
	}

	@Override
	public List<CertificateMapping> getCertificateMappings() throws CertificateMappingServiceException
	{
		try
		{
			log.debug("SetCertificateMapping");
			return certificateMapper.getCertificates();
		}
		catch (Exception e)
		{
			log.error("SetCertificateMapping",e);
			throw new CertificateMappingServiceException(e);
		}
	}
}
