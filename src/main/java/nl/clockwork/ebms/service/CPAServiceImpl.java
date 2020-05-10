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
package nl.clockwork.ebms.service;

import java.security.cert.X509Certificate;
import java.util.List;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.apachecommons.CommonsLog;
import nl.clockwork.ebms.common.JAXBParser;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CertificateMapper;
import nl.clockwork.ebms.cpa.URLMapper;
import nl.clockwork.ebms.service.model.CertificateMapping;
import nl.clockwork.ebms.service.model.URLMapping;
import nl.clockwork.ebms.validation.CPAValidator;
import nl.clockwork.ebms.validation.XSDValidator;

@CommonsLog
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class CPAServiceImpl implements CPAService
{
  @NonNull
	CPAManager cpaManager;
  @NonNull
	URLMapper urlMapper;
  @NonNull
	CertificateMapper certificateMapper;
  @NonNull
	CPAValidator cpaValidator;
	XSDValidator xsdValidator = new XSDValidator("/nl/clockwork/ebms/xsd/cpp-cpa-2_0.xsd");
	Object cpaMonitor = new Object();

	@Override
	public void validateCPA(/*CollaborationProtocolAgreement*/String cpa) throws CPAServiceException
	{
		try
		{
			log.info("ValidateCPA");
			xsdValidator.validate(cpa);
			val cpa_ = JAXBParser.getInstance(CollaborationProtocolAgreement.class).handle(cpa);
			log.info("Validating CPA " + cpa_.getCpaid());
			cpaValidator.validate(cpa_);
			log.info("ValidateCPA done");
		}
		catch (Exception e)
		{
			log.warn("ValidateCPA error",e);
			throw new CPAServiceException(e);
		}
	}
	
	@Override
	public String insertCPA(/*CollaborationProtocolAgreement*/String cpa, Boolean overwrite) throws CPAServiceException
	{
		try
		{
			log.info("InsertCPA");
			xsdValidator.validate(cpa);
			val cpa_ = JAXBParser.getInstance(CollaborationProtocolAgreement.class).handle(cpa);
			log.info("Inserting CPA " + cpa_.getCpaid());
			val currentValidator = new CPAValidator(cpaManager);
			currentValidator.validate(cpa_);
			synchronized (cpaMonitor)
			{
				if (cpaManager.existsCPA(cpa_.getCpaid()))
				{
					if (overwrite != null && overwrite)
					{
						if (cpaManager.updateCPA(cpa_) == 0)
							throw new IllegalArgumentException("Could not update CPA " + cpa_.getCpaid() + "! CPA does not exists.");
					}
					else
						throw new IllegalArgumentException("Did not insert CPA " + cpa_.getCpaid() + "! CPA already exists.");
				}
				else
					cpaManager.insertCPA(cpa_);
			}
			log.info("InsertCPA done");
			return cpa_.getCpaid();
		}
		catch (Exception e)
		{
			log.warn("InsertCPA error",e);
			throw new CPAServiceException(e);
		}
	}

	@Override
	public void deleteCPA(String cpaId) throws CPAServiceException
	{
		try
		{
			log.info("DeleteCPA " + cpaId);
			synchronized(cpaMonitor)
			{
				if (cpaManager.deleteCPA(cpaId) == 0)
					throw new IllegalArgumentException("Could not delete CPA " + cpaId + "! CPA does not exists.");
			}
			log.info("DeleteCPA " + cpaId + " done");
		}
		catch (Exception e)
		{
			log.warn("DeleteCPA " + cpaId + " error",e);
			throw new CPAServiceException(e);
		}
	}

	@Override
	public List<String> getCPAIds() throws CPAServiceException
	{
		try
		{
			log.info("GetCPAIds");
			val result = cpaManager.getCPAIds();
			log.info("GetCPAIds done");
			return result;
		}
		catch (Exception e)
		{
			log.info("GetCPAIds error",e);
			throw new CPAServiceException(e);
		}
	}

	@Override
	public /*CollaborationProtocolAgreement*/String getCPA(String cpaId) throws CPAServiceException
	{
		try
		{
			log.info("GetCPAId " + cpaId);
			val result = JAXBParser.getInstance(CollaborationProtocolAgreement.class).handle(cpaManager.getCPA(cpaId).orElse(null));
			log.info("GetCPAId " + cpaId + " done");
			return result;
		}
		catch (Exception e)
		{
			log.info("GetCPAId " + cpaId + " error",e);
			throw new CPAServiceException(e);
		}
	}

	@Override
	public void setURLMapping(URLMapping urlMapping) throws CPAServiceException
	{
		try
		{
			log.info("SetURLMapping " + urlMapping.getSource());
			urlMapper.setURLMapping(urlMapping);
			log.info("SetURLMapping " + urlMapping.getSource() + " done");
		}
		catch (Exception e)
		{
			log.info("SetURLMapping " + urlMapping.getSource() + " error",e);
			throw new CPAServiceException(e);
		}
	}

	@Override
	public void deleteURLMapping(String source) throws CPAServiceException
	{
		try
		{
			log.info("DeleteURLMapping " + source);
			urlMapper.deleteURLMapping(source);
			log.info("DeleteURLMapping " + source + " done");
		}
		catch (Exception e)
		{
			log.info("DeleteURLMapping " + source + " error",e);
			throw new CPAServiceException(e);
		}
	}

	@Override
	public List<URLMapping> getURLMappings() throws CPAServiceException
	{
		try
		{
			log.info("GetURLMappings");
			val result = urlMapper.getURLs();
			log.info("GetURLMappings done");
			return result;
		}
		catch (Exception e)
		{
			log.info("GetURLMappings error",e);
			throw new CPAServiceException(e);
		}
	}

	@Override
	public void setCertificateMapping(CertificateMapping certificateMapping) throws CPAServiceException
	{
		try
		{
			log.info("SetCertificateMapping" + certificateMapping.getSource().getSubjectDN());
			certificateMapper.setCertificateMapping(certificateMapping);
			log.info("SetCertificateMapping" + certificateMapping.getSource().getSubjectDN() + " done");
		}
		catch (Exception e)
		{
			log.info("SetCertificateMapping" + certificateMapping.getSource().getSubjectDN() + " error",e);
			throw new CPAServiceException(e);
		}
	}

	@Override
	public void deleteCertificateMapping(X509Certificate source) throws CPAServiceException
	{
		try
		{
			log.info("SetCertificateMapping" + source.getSubjectDN());
			certificateMapper.deleteCertificateMapping(source);
			log.info("SetCertificateMapping" + source.getSubjectDN() + " done");
		}
		catch (Exception e)
		{
			log.info("SetCertificateMapping" + source.getSubjectDN() + " error",e);
			throw new CPAServiceException(e);
		}
	}

	@Override
	public List<CertificateMapping> getCertificateMappings() throws CPAServiceException
	{
		try
		{
			log.info("SetCertificateMapping");
			val result = certificateMapper.getCertificates();
			log.info("SetCertificateMapping done");
			return result;
		}
		catch (Exception e)
		{
			log.info("SetCertificateMapping error",e);
			throw new CPAServiceException(e);
		}
	}
}
