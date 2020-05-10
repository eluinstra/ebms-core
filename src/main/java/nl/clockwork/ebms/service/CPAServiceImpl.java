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

import javax.xml.bind.JAXBException;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.apachecommons.CommonsLog;
import nl.clockwork.ebms.common.InvalidURLException;
import nl.clockwork.ebms.common.JAXBParser;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CertificateMapper;
import nl.clockwork.ebms.cpa.URLMapper;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.service.model.CertificateMapping;
import nl.clockwork.ebms.service.model.URLMapping;
import nl.clockwork.ebms.validation.CPAValidator;
import nl.clockwork.ebms.validation.ValidatorException;
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
			xsdValidator.validate(cpa);
			val cpa_ = JAXBParser.getInstance(CollaborationProtocolAgreement.class).handle(cpa);
			cpaValidator.validate(cpa_);
		}
		catch (JAXBException | ValidatorException e)
		{
			log.warn("",e);
			throw new CPAServiceException(e);
		}
	}
	
	@Override
	public String insertCPA(/*CollaborationProtocolAgreement*/String cpa, Boolean overwrite) throws CPAServiceException
	{
		try
		{
			xsdValidator.validate(cpa);
			val cpa_ = JAXBParser.getInstance(CollaborationProtocolAgreement.class).handle(cpa);
			val currentValidator = new CPAValidator(cpaManager);
			currentValidator.validate(cpa_);
			synchronized (cpaMonitor)
			{
				if (cpaManager.existsCPA(cpa_.getCpaid()))
				{
					if (overwrite != null && overwrite)
					{
						if (cpaManager.updateCPA(cpa_) == 0)
							throw new CPAServiceException("Could not update CPA " + cpa_.getCpaid() + "! CPA does not exists.");
					}
					else
						throw new CPAServiceException("Did not insert CPA " + cpa_.getCpaid() + "! CPA already exists.");
				}
				else
					cpaManager.insertCPA(cpa_);
			}
			return cpa_.getCpaid();
		}
		catch (JAXBException | ValidatorException | DAOException e)
		{
			log.warn("",e);
			throw new CPAServiceException(e);
		}
	}

	@Override
	public void deleteCPA(String cpaId) throws CPAServiceException
	{
		try
		{
			synchronized(cpaMonitor)
			{
				if (cpaManager.deleteCPA(cpaId) == 0)
					throw new CPAServiceException("Could not delete CPA " + cpaId + "! CPA does not exists.");
			}
		}
		catch (DAOException e)
		{
			throw new CPAServiceException(e);
		}
	}

	@Override
	public List<String> getCPAIds() throws CPAServiceException
	{
		try
		{
			return cpaManager.getCPAIds();
		}
		catch (DAOException e)
		{
			throw new CPAServiceException(e);
		}
	}

	@Override
	public /*CollaborationProtocolAgreement*/String getCPA(String cpaId) throws CPAServiceException
	{
		try
		{
			return JAXBParser.getInstance(CollaborationProtocolAgreement.class).handle(cpaManager.getCPA(cpaId).orElse(null));
		}
		catch (DAOException | JAXBException e)
		{
			throw new CPAServiceException(e);
		}
	}

	@Override
	public void setURLMapping(URLMapping urlMapping) throws CPAServiceException
	{
		try
		{
			urlMapper.setURLMapping(urlMapping);
		}
		catch (InvalidURLException e)
		{
			throw new CPAServiceException(e);
		}
	}

	@Override
	public void deleteURLMapping(String source) throws CPAServiceException
	{
		urlMapper.deleteURLMapping(source);
	}

	@Override
	public List<URLMapping> getURLMappings() throws CPAServiceException
	{
		return urlMapper.getURLs();
	}

	@Override
	public void setCertificateMapping(CertificateMapping certificateMapping) throws CPAServiceException
	{
		certificateMapper.setCertificateMapping(certificateMapping);
	}

	@Override
	public void deleteCertificateMapping(X509Certificate source) throws CPAServiceException
	{
		certificateMapper.deleteCertificateMapping(source);
	}

	@Override
	public List<CertificateMapping> getCertificateMappings() throws CPAServiceException
	{
		return certificateMapper.getCertificates();
	}
}
