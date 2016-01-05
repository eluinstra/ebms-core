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

import java.util.List;

import javax.xml.bind.JAXBException;

import nl.clockwork.ebms.common.CPAManager;
import nl.clockwork.ebms.common.XMLMessageBuilder;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.validation.CPAValidator;
import nl.clockwork.ebms.validation.ValidatorException;
import nl.clockwork.ebms.validation.XSDValidator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;

public class CPAServiceImpl implements CPAService
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private XSDValidator xsdValidator;
	private CPAManager cpaManager;
	private Object cpaMonitor = new Object();

	public CPAServiceImpl()
	{
		xsdValidator = new XSDValidator("/nl/clockwork/ebms/xsd/cpp-cpa-2_0.xsd");
	}
	
	@Override
	public
	void validateCPA(/*CollaborationProtocolAgreement*/String cpa) throws CPAServiceException
	{
		try
		{
			xsdValidator.validate(cpa);
			CollaborationProtocolAgreement cpa_ = XMLMessageBuilder.getInstance(CollaborationProtocolAgreement.class).handle(cpa);
			new CPAValidator().validate(cpa_);
		}
		catch (JAXBException | ValidatorException e)
		{
			logger.warn("",e);
			throw new CPAServiceException(e);
		}
	}
	
	@Override
	public String insertCPA(/*CollaborationProtocolAgreement*/String cpa, Boolean overwrite) throws CPAServiceException
	{
		try
		{
			xsdValidator.validate(cpa);
			CollaborationProtocolAgreement cpa_ = XMLMessageBuilder.getInstance(CollaborationProtocolAgreement.class).handle(cpa);
			new CPAValidator().validate(cpa_);
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
			logger.warn("",e);
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
			return XMLMessageBuilder.getInstance(CollaborationProtocolAgreement.class).handle(cpaManager.getCPA(cpaId));
		}
		catch (DAOException | JAXBException e)
		{
			throw new CPAServiceException(e);
		}
	}

	public void setCpaManager(CPAManager cpaManager)
	{
		this.cpaManager = cpaManager;
	}

	@Override
	public String getURL(String cpaId) throws CPAServiceException
	{
		return cpaManager.getUrl(cpaId);
	}

	@Override
	public void setURL(String cpaId, String url) throws CPAServiceException
	{
		cpaManager.setUrl(cpaId,url);
	}
}
