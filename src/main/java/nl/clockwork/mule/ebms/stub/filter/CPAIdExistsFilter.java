/*******************************************************************************
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
 ******************************************************************************/
package nl.clockwork.mule.ebms.stub.filter;

import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.iface.CPAService;
import nl.clockwork.mule.ebms.stub.Constants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;

public class CPAIdExistsFilter implements Filter
{
	protected transient Log logger = LogFactory.getLog(getClass());
	private CPAService cpaService;

	@Override
	public boolean accept(MuleMessage message)
	{
		try
		{
			String cpaId = (String)message.getProperty(Constants.CPA_ID);
			boolean result = cpaService.getCPA(cpaId) != null;
			return result;
		}
		catch (DAOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public void setCpaService(CPAService cpaService)
	{
		this.cpaService = cpaService;
	}
}
