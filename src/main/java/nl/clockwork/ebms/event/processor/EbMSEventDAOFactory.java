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

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.dao.AbstractDAOFactory;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class EbMSEventDAOFactory extends AbstractDAOFactory<EbMSEventDAO>
{
	private static class DB2EbMSEventDAO extends EbMSEventDAOImpl
	{
		public DB2EbMSEventDAO(@NonNull JdbcTemplate jdbcTemplate)
		{
			super(jdbcTemplate);
		}

		@Override
		public String getEventsBeforeQuery(int maxNr, String serverId)
		{
			return "select cpa_id, send_channel_id, receive_channel_id, message_id, time_to_live, time_stamp, is_confidential, retries" +
					" from ebms_event" +
					" where time_stamp <= ?" +
					(serverId == null ? " and server_id is null" : " and server_id = '" + serverId + "'") +
					" order by time_stamp asc" +
					" fetch first " + maxNr + " rows only";
		}
	}
	private static class H2EbMSEventDAO extends EbMSEventDAOImpl
	{
		public H2EbMSEventDAO(@NonNull JdbcTemplate jdbcTemplate)
		{
			super(jdbcTemplate);
		}

		@Override
		public String getEventsBeforeQuery(int maxNr, String serverId)
		{
			return "select cpa_id, send_channel_id, receive_channel_id, message_id, time_to_live, time_stamp, is_confidential, retries" +
					" from ebms_event" +
					" where time_stamp <= ?" +
					(serverId == null ? " and server_id is null" : " and server_id = '" + serverId + "'") +
					" order by time_stamp asc" +
					" fetch first " + maxNr + " rows only";
		}
	}
	private static class HSQLDBEbMSEventDAO extends EbMSEventDAOImpl
	{
		public HSQLDBEbMSEventDAO(@NonNull JdbcTemplate jdbcTemplate)
		{
			super(jdbcTemplate);
		}

		@Override
		public String getEventsBeforeQuery(int maxNr, String serverId)
		{
			return "select cpa_id, send_channel_id, receive_channel_id, message_id, time_to_live, time_stamp, is_confidential, retries" +
					" from ebms_event" +
					" where time_stamp <= ?" +
					(serverId == null ? " and server_id is null" : " and server_id = '" + serverId + "'") +
					" order by time_stamp asc" +
					" limit " + maxNr;
		}
	}
	private static class MSSQLEbMSEventDAO extends EbMSEventDAOImpl
	{
		public MSSQLEbMSEventDAO(@NonNull JdbcTemplate jdbcTemplate)
		{
			super(jdbcTemplate);
		}

		@Override
		public String getEventsBeforeQuery(int maxNr, String serverId)
		{
			return "select top " + maxNr + " cpa_id, send_channel_id, receive_channel_id, message_id, time_to_live, time_stamp, is_confidential, retries" +
					" from ebms_event" +
					" where time_stamp <= ?" +
					(serverId == null ? " and server_id is null" : " and server_id = '" + serverId + "'") +
					" order by time_stamp asc";
		}
	}
	private static class MySQLEbMSEventDAO extends EbMSEventDAOImpl
	{
		public MySQLEbMSEventDAO(@NonNull JdbcTemplate jdbcTemplate)
		{
			super(jdbcTemplate);
		}

		@Override
		public String getEventsBeforeQuery(int maxNr, String serverId)
		{
			return "select cpa_id, send_channel_id, receive_channel_id, message_id, time_to_live, time_stamp, is_confidential, retries" +
					" from ebms_event" +
					" where time_stamp <= ?" +
					(serverId == null ? " and server_id is null" : " and server_id = '" + serverId + "'") +
					" order by time_stamp asc" +
					" limit " + maxNr;
		}
	}
	private static class OracleEbMSEventDAO extends EbMSEventDAOImpl
	{
		public OracleEbMSEventDAO(@NonNull JdbcTemplate jdbcTemplate)
		{
			super(jdbcTemplate);
		}

		@Override
		public String getEventsBeforeQuery(int maxNr, String serverId)
		{
			return "select * from (" +
					"select cpa_id, send_channel_id, receive_channel_id, message_id, time_to_live, time_stamp, is_confidential, retries" +
					" from ebms_event" +
					" where time_stamp <= ?" +
					(serverId == null ? " and server_id is null" : " and server_id = '" + serverId + "'") +
					" order by time_stamp asc)" +
					" where ROWNUM <= " + maxNr;
		}
	}
	private static class PostgreSQLEbMSEventDAO extends EbMSEventDAOImpl
	{
		public PostgreSQLEbMSEventDAO(@NonNull JdbcTemplate jdbcTemplate)
		{
			super(jdbcTemplate);
		}

		@Override
		public String getEventsBeforeQuery(int maxNr, String serverId)
		{
			return "select cpa_id, send_channel_id, receive_channel_id, message_id, time_to_live, time_stamp, is_confidential, retries" +
					" from ebms_event" +
					" where time_stamp <= ?" +
					(serverId == null ? " and server_id is null" : " and server_id = '" + serverId + "'") +
					" order by time_stamp asc" +
					" limit " + maxNr;
		}
	}
	@NonNull
	JdbcTemplate jdbcTemplate;

	public EbMSEventDAOFactory(DataSource dataSource,	@NonNull JdbcTemplate jdbcTemplate)
	{
		super(dataSource);
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public Class<EbMSEventDAO> getObjectType()
	{
		return EbMSEventDAO.class;
	}

	@Override
	public EbMSEventDAO createDB2DAO()
	{
		return new DB2EbMSEventDAO(jdbcTemplate);
	}

	@Override
	public EbMSEventDAO createH2DAO()
	{
		return new H2EbMSEventDAO(jdbcTemplate);
	}

	@Override
	public EbMSEventDAO createHSQLDBDAO()
	{
		return new HSQLDBEbMSEventDAO(jdbcTemplate);
	}

	@Override
	public EbMSEventDAO createMSSQLDAO()
	{
		return new MSSQLEbMSEventDAO(jdbcTemplate);
	}

	@Override
	public EbMSEventDAO createMySQLDAO()
	{
		return new MySQLEbMSEventDAO(jdbcTemplate);
	}

	@Override
	public EbMSEventDAO createOracleDAO()
	{
		return new OracleEbMSEventDAO(jdbcTemplate);
	}

	@Override
	public EbMSEventDAO createPostgreSQLDAO()
	{
		return new PostgreSQLEbMSEventDAO(jdbcTemplate);
	}
}
