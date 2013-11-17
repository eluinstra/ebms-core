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
package nl.clockwork.ebms.dao.oracle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.dao.AbstractEbMSDAO;
import nl.clockwork.ebms.dao.ConnectionManager;

public class EbMSDAOImpl extends AbstractEbMSDAO
{
	public EbMSDAOImpl(ConnectionManager connectionManager)
	{
		super(connectionManager);
	}

//	@Override
//	public String getDateFormat()
//	{
//		return "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL";
//	}

	@Override
	public String getTimestampFunction()
	{
		return "SYSDATE";
	}

	@Override
	public String getMessageIdsQuery(String messageContextFilter, EbMSMessageStatus status, int maxNr)
	{
		return "select * from (" +
		"select message_id" +
		" from ebms_message" +
		" where message_nr = 0" +
		" and status = " + status.id() +
		messageContextFilter +
		" order by time_stamp asc)" +
		" where ROWNUM <= " + maxNr;
	}

	@Override
	protected PreparedStatement getInsertMessagePreparedStatement(Connection connection, EbMSMessageStatus status) throws SQLException
	{
		return connection.prepareStatement
		(
			"insert into ebms_message (" +
				"id," +
				"time_stamp," +
				"cpa_id," +
				"conversation_id," +
				"sequence_nr," +
				"message_id," +
				"ref_to_message_id," +
				"time_to_live," +
				"from_role," +
				"to_role," +
				"service," +
				"action," +
				"signature," +
				"message_header," +
				"sync_reply," +
				"message_order," +
				"ack_requested," +
				"content," +
				"status," +
				"status_time" +
			") values (seq_ebms_message_id.nextval,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?," + (status == null ? "null" : getTimestampFunction()) + ")",
			//new String[]{"id"}
			new int[]{1}
		);
	}

	@Override
	protected PreparedStatement getInsertDuplicateMessagePreparedStatement(Connection connection) throws SQLException
	{
		return connection.prepareStatement
		(
			"insert into ebms_message (" +
				"id," +
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
				"service," +
				"action," +
				"signature," +
				"message_header," +
				"sync_reply," +
				"message_order," +
				"ack_requested," +
				"content" +
			") values (seq_ebms_message_id.nextval,?,?,?,?,?,(select max(message_nr) + 1 from ebms_message where message_id = ?),?,?,?,?,?,?,?,?,?,?,?,?)",
			//new String[]{"id"}
			new int[]{1}
		);
	}

}
