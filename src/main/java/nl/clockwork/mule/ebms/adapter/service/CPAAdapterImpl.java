package nl.clockwork.mule.ebms.adapter.service;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import nl.clockwork.common.dao.DAOException;
import nl.clockwork.common.util.XMLMessageBuilder;
import nl.clockwork.mule.ebms.dao.EbMSDAO;
import nl.clockwork.mule.ebms.model.cpp.cpa.CollaborationProtocolAgreement;

public class CPAAdapterImpl implements CPAAdapter
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private EbMSDAO ebMSDAO;

	@Override
	public boolean insertCPA(/*CollaborationProtocolAgreement*/String cpa_, Boolean overwrite)
	{
		try
		{
			CollaborationProtocolAgreement cpa = XMLMessageBuilder.getInstance(CollaborationProtocolAgreement.class).handle(cpa_);
			boolean result = ebMSDAO.insertCPA(cpa);
			if (!result && (overwrite == null || overwrite == true))
				result = ebMSDAO.updateCPA(cpa);
			return result;
		}
		catch (DAOException e)
		{
			//throw new CPAAdapterException(e);
			logger.warn("",e);
			return false;
		}
		catch (JAXBException e)
		{
			//throw new CPAAdapterException(e);
			logger.warn("",e);
			return false;
		}
	}

	@Override
	public boolean deleteCPA(String cpaId)
	{
		try
		{
			return ebMSDAO.deleteCPA(cpaId);
		}
		catch (DAOException e)
		{
			//throw new CPAAdapterException(e);
			logger.warn("",e);
			return false;
		}
	}

	@Override
	public List<String> getCPAIds()
	{
		try
		{
			return ebMSDAO.getCPAIds();
		}
		catch (DAOException e)
		{
			//throw new CPAAdapterException(e);
			logger.warn("",e);
			return new ArrayList<String>();
		}
	}

	@Override
	public /*CollaborationProtocolAgreement*/String getCPA(String cpaId)
	{
		try
		{
			return XMLMessageBuilder.getInstance(CollaborationProtocolAgreement.class).handle(ebMSDAO.getCPA(cpaId));
		}
		catch (DAOException e)
		{
			//throw new CPAAdapterException(e);
			logger.warn("",e);
			return null;
		}
		catch (JAXBException e)
		{
			//throw new CPAAdapterException(e);
			logger.warn("",e);
			return null;
		}
	}
	
	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}

}
