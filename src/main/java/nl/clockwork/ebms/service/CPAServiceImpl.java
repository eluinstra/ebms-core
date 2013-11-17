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

import nl.clockwork.ebms.common.XMLMessageBuilder;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.dao.EbMSDAO;
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
	private EbMSDAO ebMSDAO;
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
		catch (JAXBException e)
		{
			logger.warn("",e);
			throw new CPAServiceException(e);
		}
		catch (ValidatorException e)
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
				if (ebMSDAO.existsCPA(cpa_.getCpaid()))
				{
					if (overwrite)
					{
						if (!ebMSDAO.updateCPA(cpa_))
							throw new CPAServiceException("Could not update CPA " + cpa_.getCpaid() + "! CPA does not exists.");
			}
					else
						throw new CPAServiceException("Did not insert CPA " + cpa_.getCpaid() + "! CPA already exists.");
				}
				else
					if (!ebMSDAO.insertCPA(cpa_))
						throw new CPAServiceException("Could not insert CPA " + cpa_.getCpaid() + "!");
			}
			return cpa_.getCpaid();
		}
		catch (JAXBException e)
		{
			logger.warn("",e);
			throw new CPAServiceException(e);
		}
		catch (ValidatorException e)
		{
			logger.warn("",e);
			throw new CPAServiceException(e);
		}
		catch (DAOException e)
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
				if (!ebMSDAO.deleteCPA(cpaId))
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
			return ebMSDAO.getCPAIds();
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
			return XMLMessageBuilder.getInstance(CollaborationProtocolAgreement.class).handle(ebMSDAO.getCPA(cpaId));
		}
		catch (DAOException e)
		{
			throw new CPAServiceException(e);
		}
		catch (JAXBException e)
		{
			throw new CPAServiceException(e);
		}
	}
	
	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}

}
