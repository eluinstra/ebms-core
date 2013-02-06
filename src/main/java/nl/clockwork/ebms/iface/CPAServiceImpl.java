package nl.clockwork.ebms.iface;

import java.util.List;

import javax.xml.bind.JAXBException;

import nl.clockwork.ebms.common.util.XMLMessageBuilder;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.cpp.cpa.CollaborationProtocolAgreement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CPAServiceImpl implements CPAService
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private EbMSDAO ebMSDAO;
	private Object cpaMonitor = new Object();

	@Override
	public boolean insertCPA(/*CollaborationProtocolAgreement*/String cpa_, Boolean overwrite) throws CPAServiceException
	{
		try
		{
			CollaborationProtocolAgreement cpa = XMLMessageBuilder.getInstance(CollaborationProtocolAgreement.class).handle(cpa_);
			boolean result = false;
			synchronized (cpaMonitor)
			{
				if (ebMSDAO.existsCPA(cpa.getCpaid()))
					if (overwrite)
						result = ebMSDAO.updateCPA(cpa);
					else
						result = ebMSDAO.insertCPA(cpa);
			}
			return result;
		}
		catch (DAOException e)
		{
			throw new CPAServiceException(e);
			//logger.warn("",e);
			//return false;
		}
		catch (JAXBException e)
		{
			throw new CPAServiceException(e);
			//logger.warn("",e);
			//return false;
		}
	}

	@Override
	public boolean deleteCPA(String cpaId) throws CPAServiceException
	{
		try
		{
			synchronized(cpaMonitor)
			{
				return ebMSDAO.deleteCPA(cpaId);
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
