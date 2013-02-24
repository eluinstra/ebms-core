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
package nl.clockwork.ebms.dao.postgresql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.dao.AbstractEbMSDAO;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

public class EbMSDAOImpl extends AbstractEbMSDAO
{
	public EbMSDAOImpl(DataSource dataSource)
	{
		super(dataSource);
	}

	public class IdExtractor implements ResultSetExtractor<Long>
	{

		@Override
		public Long extractData(ResultSet rs) throws SQLException, DataAccessException
		{
			if (rs.next())
				return rs.getLong("id");
			else
				return null;
		}
		
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
	public String getMessageIdsQuery(String messageContextFilter, EbMSMessageStatus status, int maxNr)
	{
		return "select message_id" +
		" from ebms_message" +
		" where status=" + status.id() +
		messageContextFilter +
		" order by time_stamp asc" +
		" limit " + maxNr;
	}

	@Override
	protected PreparedStatement getInsertMessagePreparedStatement(Connection connection, EbMSMessageStatus status) throws SQLException
	{
		return connection.prepareStatement
		(
			"insert into ebms_message (" +
				"time_stamp," +
				"cpa_id," +
				"conversation_id," +
				"sequence_nr," +
				"message_id," +
				"ref_to_message_id," +
				"time_to_live," +
				"from_role," +
				"to_role," +
				"service_type," +
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
			") values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?," + (status == null ? "null" : getTimestampFunction()) + ")",
			Statement.RETURN_GENERATED_KEYS
		);
	}

}
