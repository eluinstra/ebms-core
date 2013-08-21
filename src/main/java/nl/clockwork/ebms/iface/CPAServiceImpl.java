package nl.clockwork.ebms.iface;

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
	public void insertCPA(/*CollaborationProtocolAgreement*/String cpa_, Boolean overwrite) throws CPAServiceException
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
						if (!ebMSDAO.updateCPA(cpa))
							throw new CPAServiceException("Could not update CPA! CPAId: " + cpa.getCpaid());
				}
				else
					if (!ebMSDAO.insertCPA(cpa))
						throw new CPAServiceException("Could not insert CPA! CPAId: " + cpa.getCpaid());
			}
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
					throw new CPAServiceException("Could not delete CPA! CPAId: " + cpaId);
			}
		}
		catch (DAOException e)
		{
			throw new CPAServiceException(e);
			//logger.warn("",e);
			//return false;
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
			//logger.warn("",e);
			//return new ArrayList<String>();
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
			//logger.warn("",e);
			//return null;
		}
		catch (JAXBException e)
		{
			throw new CPAServiceException(e);
			//logger.warn("",e);
			//return null;
		}
	}
	
	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}

}
