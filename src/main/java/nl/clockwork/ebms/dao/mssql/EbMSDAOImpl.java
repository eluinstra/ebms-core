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
package nl.clockwork.ebms.dao.mssql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.dao.ConnectionManager;

public class EbMSDAOImpl extends nl.clockwork.ebms.dao.mysql.EbMSDAOImpl
{
	public EbMSDAOImpl(ConnectionManager connectionManager)
	{
		super(connectionManager);
	}

//@Override
//public String getDateFormat()
//{
//	return "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS:%1$tL";
//}

	@Override
	public String getTimestampFunction()
	{
		return "GETDATE()";
	}

	@Override
	public String getMessageIdsQuery(String messageContextFilter, EbMSMessageStatus status, int maxNr)
	{
		return "select top " + maxNr + " message_id" +
		" from ebms_message" +
		" where message_nr = 0" +
		" and status = " + status.id() +
		messageContextFilter +
		" order by time_stamp asc";
//		return "select message_id" +
//		" from (" +
//			" select message_id, ROW_NUMBER() OVER (order by time_stamp asc) as rownum" +
//			" from ebms_message" +
//			" where message_nr = 0" +
//			" and status = " + EbMSMessageStatus.RECEIVED.id() +
//			messageContextFilter +
//		" ) as tmpTable" +
//		//" where tmpTable.rownum between 0 and " + maxNr;
//		" where tmpTable.rownum < " + maxNr;
	}

	protected PreparedStatement getInsertDuplicateMessagePreparedStatement(Connection connection) throws SQLException
	{
		return connection.prepareStatement
		(
			"insert into ebms_message (" +
				"time_stamp," +
				"cpa_id," +
				"conversation_id," +
				"sequence_nr," +
				"message_id," +
				"message_nr," +
				"ref_to_message_id," +
				"time_to_live," +
				"from_role," +
				"to_role," +
				"service_type," +
				"service," +
				"action," +
				"content" +
			") values (?,?,?,?,?,(select max(message_nr) + 1 as nr from ebms_message where message_id = ?),?,?,?,?,?,?,?,?)",
			//new String[]{"id"}
			new int[]{1}
		);
	}

}
