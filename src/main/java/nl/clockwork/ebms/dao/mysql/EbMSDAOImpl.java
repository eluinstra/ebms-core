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
package nl.clockwork.ebms.dao.mysql;

import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.dao.AbstractEbMSDAO;
import nl.clockwork.ebms.dao.ConnectionManager;

public class EbMSDAOImpl extends AbstractEbMSDAO
{
	public EbMSDAOImpl(ConnectionManager connectionManager)
	{
		super(connectionManager);
	}

//@Override
//public String getDateFormat()
//{
//	return "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS";
//}

	@Override
	public String getTimestampFunction()
	{
		return "NOW()";
	}

	@Override
	public String getMessageIdsQuery(String messageContextFilter, EbMSMessageStatus status, int maxNr)
	{
		return "select message_id" +
		" from ebms_message" +
		" where message_nr = 0" +
		" and status = " + status.id() +
		messageContextFilter +
		" order by time_stamp asc" +
		" limit " + maxNr;
	}
}
