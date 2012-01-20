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
package nl.clockwork.mule.ebms.dao.postgresql;

import nl.clockwork.mule.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.mule.ebms.dao.AbstractEbMSDAO;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class EbMSDAOImpl extends AbstractEbMSDAO
{
	public EbMSDAOImpl(PlatformTransactionManager transactionManager, javax.sql.DataSource dataSource)
	{
		super(transactionManager,dataSource);
	}

	public EbMSDAOImpl(TransactionTemplate transactionTemplate, javax.sql.DataSource dataSource)
	{
		super(transactionTemplate,dataSource);
	}

//	@Override
//	public String getDateFormat()
//	{
//		return "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL";
//	}

	@Override
	public String getTimestampFunction()
	{
		return "NOW()";
	}

	@Override
	public String getMessageIdsQuery(int maxNr)
	{
		return "select message_id" +
		" from ebms_message" +
		" where status=" + EbMSMessageStatus.RECEIVED.id() +
		" order by time_stamp asc" +
		" limit " + maxNr;
//		return "select * from (" +
//		"select message_id" +
//		" from ebms_message" +
//		" where status=" + EbMSMessageStatus.RECEIVED.id() +
//		" order by time_stamp asc)" +
//		" limit " + maxNr;
	}

}
