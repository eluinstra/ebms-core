package nl.clockwork.ebms.service;

import java.util.List;

import javax.xml.bind.JAXBException;

import nl.clockwork.ebms.common.XMLMessageBuilder;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.validation.CPAValidator;
import nl.clockwork.ebms.validation.ValidatorException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;

public class CPAServiceImpl implements CPAService
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private EbMSDAO ebMSDAO;
	private Object cpaMonitor = new Object();

	@Override
	public
	void validateCPA(/*CollaborationProtocolAgreement*/String cpa_) throws CPAServiceException
	{
		try
		{
			CollaborationProtocolAgreement cpa = XMLMessageBuilder.getInstance(CollaborationProtocolAgreement.class).handle(cpa_);
			new CPAValidator().validate(cpa);
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
	public String insertCPA(/*CollaborationProtocolAgreement*/String cpa_, Boolean overwrite) throws CPAServiceException
	{
		try
		{
			CollaborationProtocolAgreement cpa = XMLMessageBuilder.getInstance(CollaborationProtocolAgreement.class).handle(cpa_);
			new CPAValidator().validate(cpa);
			synchronized (cpaMonitor)
			{
				if (ebMSDAO.existsCPA(cpa.getCpaid()))
				{
					if (overwrite)
					{
						if (!ebMSDAO.updateCPA(cpa))
							throw new CPAServiceException("Could not update CPA! CPA does not exists! CPAId: " + cpa.getCpaid());
					}
					else
						throw new CPAServiceException("Did not insert CPA! CPA already exists! CPAId: " + cpa.getCpaid());
				}
				else
					if (!ebMSDAO.insertCPA(cpa))
						throw new CPAServiceException("Could not insert CPA! CPAId: " + cpa.getCpaid());
			}
			return cpa.getCpaid();
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
					throw new CPAServiceException("Could not delete CPA! CPA does not exists!");
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
