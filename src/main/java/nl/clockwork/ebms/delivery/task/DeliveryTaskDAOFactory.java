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
package nl.clockwork.ebms.delivery.task;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.dao.AbstractDAOFactory;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class DeliveryTaskDAOFactory extends AbstractDAOFactory<DeliveryTaskDAO>
{
	private static class DB2DeliveryTaskDAO extends DeliveryTaskDAOImpl
	{
		public DB2DeliveryTaskDAO(@NonNull JdbcTemplate jdbcTemplate)
		{
			super(jdbcTemplate);
		}

		@Override
		public String getTasksBeforeQuery(int maxNr, String serverId)
		{
			return "select cpa_id, send_channel_id, receive_channel_id, message_id, time_to_live, time_stamp, is_confidential, retries" +
					" from delivery_task" +
					" where time_stamp <= ?" +
					(serverId == null ? " and server_id is null" : " and server_id = '" + serverId + "'") +
					" order by time_stamp asc" +
					" fetch first " + maxNr + " rows only";
		}
	}
	private static class H2DeliveryTaskDAO extends DeliveryTaskDAOImpl
	{
		public H2DeliveryTaskDAO(@NonNull JdbcTemplate jdbcTemplate)
		{
			super(jdbcTemplate);
		}

		@Override
		public String getTasksBeforeQuery(int maxNr, String serverId)
		{
			return "select cpa_id, send_channel_id, receive_channel_id, message_id, time_to_live, time_stamp, is_confidential, retries" +
					" from delivery_task" +
					" where time_stamp <= ?" +
					(serverId == null ? " and server_id is null" : " and server_id = '" + serverId + "'") +
					" order by time_stamp asc" +
					" limit " + maxNr;
		}
	}
	private static class HSQLDBDeliveryTaskDAO extends DeliveryTaskDAOImpl
	{
		public HSQLDBDeliveryTaskDAO(@NonNull JdbcTemplate jdbcTemplate)
		{
			super(jdbcTemplate);
		}

		@Override
		public String getTasksBeforeQuery(int maxNr, String serverId)
		{
			return "select cpa_id, send_channel_id, receive_channel_id, message_id, time_to_live, time_stamp, is_confidential, retries" +
					" from delivery_task" +
					" where time_stamp <= ?" +
					(serverId == null ? " and server_id is null" : " and server_id = '" + serverId + "'") +
					" order by time_stamp asc" +
					" limit " + maxNr;
		}
	}
	private static class MSSQLDeliveryTaskDAO extends DeliveryTaskDAOImpl
	{
		public MSSQLDeliveryTaskDAO(@NonNull JdbcTemplate jdbcTemplate)
		{
			super(jdbcTemplate);
		}

		@Override
		public String getTasksBeforeQuery(int maxNr, String serverId)
		{
			return "select top " + maxNr + " cpa_id, send_channel_id, receive_channel_id, message_id, time_to_live, time_stamp, is_confidential, retries" +
					" from delivery_task" +
					" where time_stamp <= ?" +
					(serverId == null ? " and server_id is null" : " and server_id = '" + serverId + "'") +
					" order by time_stamp asc";
		}
	}
	private static class MySQLDeliveryTaskDAO extends DeliveryTaskDAOImpl
	{
		public MySQLDeliveryTaskDAO(@NonNull JdbcTemplate jdbcTemplate)
		{
			super(jdbcTemplate);
		}

		@Override
		public String getTasksBeforeQuery(int maxNr, String serverId)
		{
			return "select cpa_id, send_channel_id, receive_channel_id, message_id, time_to_live, time_stamp, is_confidential, retries" +
					" from delivery_task" +
					" where time_stamp <= ?" +
					(serverId == null ? " and server_id is null" : " and server_id = '" + serverId + "'") +
					" order by time_stamp asc" +
					" limit " + maxNr;
		}
	}
	private static class OracleDeliveryTaskDAO extends DeliveryTaskDAOImpl
	{
		public OracleDeliveryTaskDAO(@NonNull JdbcTemplate jdbcTemplate)
		{
			super(jdbcTemplate);
		}

		@Override
		public String getTasksBeforeQuery(int maxNr, String serverId)
		{
			return "select * from (" +
					"select cpa_id, send_channel_id, receive_channel_id, message_id, time_to_live, time_stamp, is_confidential, retries" +
					" from delivery_task" +
					" where time_stamp <= ?" +
					(serverId == null ? " and server_id is null" : " and server_id = '" + serverId + "'") +
					" order by time_stamp asc)" +
					" where ROWNUM <= " + maxNr;
		}
	}
	private static class PostgreSQLDeliveryTaskDAO extends DeliveryTaskDAOImpl
	{
		public PostgreSQLDeliveryTaskDAO(@NonNull JdbcTemplate jdbcTemplate)
		{
			super(jdbcTemplate);
		}

		@Override
		public String getTasksBeforeQuery(int maxNr, String serverId)
		{
			return "select cpa_id, send_channel_id, receive_channel_id, message_id, time_to_live, time_stamp, is_confidential, retries" +
					" from delivery_task" +
					" where time_stamp <= ?" +
					(serverId == null ? " and server_id is null" : " and server_id = '" + serverId + "'") +
					" order by time_stamp asc" +
					" limit " + maxNr;
		}
	}
	@NonNull
	JdbcTemplate jdbcTemplate;

	public DeliveryTaskDAOFactory(DataSource dataSource,	@NonNull JdbcTemplate jdbcTemplate)
	{
		super(dataSource);
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public Class<DeliveryTaskDAO> getObjectType()
	{
		return DeliveryTaskDAO.class;
	}

	@Override
	public DeliveryTaskDAO createDB2DAO()
	{
		return new DB2DeliveryTaskDAO(jdbcTemplate);
	}

	@Override
	public DeliveryTaskDAO createH2DAO()
	{
		return new H2DeliveryTaskDAO(jdbcTemplate);
	}

	@Override
	public DeliveryTaskDAO createHSQLDBDAO()
	{
		return new HSQLDBDeliveryTaskDAO(jdbcTemplate);
	}

	@Override
	public DeliveryTaskDAO createMSSQLDAO()
	{
		return new MSSQLDeliveryTaskDAO(jdbcTemplate);
	}

	@Override
	public DeliveryTaskDAO createMySQLDAO()
	{
		return new MySQLDeliveryTaskDAO(jdbcTemplate);
	}

	@Override
	public DeliveryTaskDAO createOracleDAO()
	{
		return new OracleDeliveryTaskDAO(jdbcTemplate);
	}

	@Override
	public DeliveryTaskDAO createPostgreSQLDAO()
	{
		return new PostgreSQLDeliveryTaskDAO(jdbcTemplate);
	}
}
