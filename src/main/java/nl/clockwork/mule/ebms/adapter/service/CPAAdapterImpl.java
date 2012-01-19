package nl.clockwork.mule.ebms.adapter.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import nl.clockwork.common.dao.DAOException;
import nl.clockwork.mule.ebms.dao.EbMSDAO;
import nl.clockwork.mule.ebms.model.cpp.cpa.CollaborationProtocolAgreement;

public class CPAAdapterImpl implements CPAAdapter
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private EbMSDAO ebMSDAO;

	@Override
	public boolean insertCPA(CollaborationProtocolAgreement cpa, Boolean overwrite)
	{
		try
		{
			boolean result = ebMSDAO.insertCPA(cpa);
			if (!result && (overwrite == null || overwrite == true))
				result = ebMSDAO.updateCPA(cpa);
			return result;
		}
		catch (DAOException e)
		{
			//throw new CPAAdapterException(e);
			logger.error("",e);
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
			logger.error("",e);
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
			logger.error("",e);
			return new ArrayList<String>();
		}
	}

	@Override
	public CollaborationProtocolAgreement getCPA(String cpaId)
	{
		try
		{
			return ebMSDAO.getCPA(cpaId);
		}
		catch (DAOException e)
		{
			//throw new CPAAdapterException(e);
			logger.error("",e);
			return null;
		}
	}

}
