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
package nl.clockwork.ebms.event.processor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.FactoryBean;

import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.dao.EbMSDAO;

public class EventManagerFactory implements FactoryBean<EventManager>
{
	private static final Log logger = LogFactory.getLog(EventManagerFactory.class);
	private EbMSDAO ebMSDAO;
	private CPAManager cpaManager;
	private boolean autoRetryResponse;
	private int nrAutoRetries;
	private int autoRetryInterval;
	
	public EbMSDAO getEbMSDAO()
	{
		return ebMSDAO;
	}
	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}
	public CPAManager getCpaManager()
	{
		return cpaManager;
	}
	public void setCpaManager(CPAManager cpaManager)
	{
		this.cpaManager = cpaManager;
	}
	public boolean isAutoRetryResponse()
	{
		return autoRetryResponse;
	}
	public void setAutoRetryResponse(boolean autoRetryResponse) 
	{
		this.autoRetryResponse = autoRetryResponse;
	}
	public int getNrAutoRetries()
	{
		return nrAutoRetries;
	}
	public void setNrAutoRetries(int nrAutoRetries)
	{
		this.nrAutoRetries = nrAutoRetries;
	}
	public int getAutoRetryInterval()
	{
		return autoRetryInterval;
	}
	public void setAutoRetryInterval(int autoRetryInterval)
	{
		this.autoRetryInterval = autoRetryInterval;
	}

	@Override
	public EventManager getObject() throws Exception
	{
		EventManager mgr = null;
		if (autoRetryResponse)
		{
			logger.info("Using EventManager RetryAck");
			mgr = new EventManagerRetryAck();
			((EventManagerRetryAck) mgr).setNrAutoRetries(nrAutoRetries);
			((EventManagerRetryAck) mgr).setAutoRetryInterval(autoRetryInterval);			
		}
		else
		{
			logger.info("Using EventManager DEFAULT");
			mgr = new EventManager();
		}
		mgr.setCpaManager(cpaManager);
		mgr.setEbMSDAO(ebMSDAO);
		
		return mgr;
	}
	
	@Override
	public Class<?> getObjectType()
	{
		return EventManager.class;
	}
	
	@Override
	public boolean isSingleton()
	{
		return true;
	}
	
}
