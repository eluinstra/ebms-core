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
package nl.clockwork.ebms.event.listener;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.dao.AbstractDAOFactory;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class EbMSMessageEventDAOFactory extends AbstractDAOFactory<EbMSMessageEventDAO>
{
	private static class DB2EbMSMessageEventDAO extends EbMSMessageEventDAOImpl
	{
		public DB2EbMSMessageEventDAO(@NonNull JdbcTemplate jdbcTemplate)
		{
			super(jdbcTemplate);
		}

		@Override
		protected String getMessageEventsQuery(String messageContextFilter, EbMSMessageEventType[] types, int maxNr)
		{
			return "select ebms_message_event.message_id, ebms_message_event.event_type" +
					" from ebms_message_event, ebms_message" +
					" where ebms_message_event.processed = 0" +
					" and ebms_message_event.event_type in (" + join(types == null ? EbMSMessageEventType.values() : types,",") + ")" +
					" and ebms_message_event.message_id = ebms_message.message_id" +
					" and ebms_message.message_nr = 0" +
					messageContextFilter +
					" order by ebms_message_event.time_stamp asc" +
					" fetch first " + maxNr + " rows only";
		}
	}
	private static class H2EbMSMessageEventDAO extends EbMSMessageEventDAOImpl
	{
		public H2EbMSMessageEventDAO(@NonNull JdbcTemplate jdbcTemplate)
		{
			super(jdbcTemplate);
		}

		@Override
		protected String getMessageEventsQuery(String messageContextFilter, EbMSMessageEventType[] types, int maxNr)
		{
			return "select ebms_message_event.message_id, ebms_message_event.event_type" +
					" from ebms_message_event, ebms_message" +
					" where ebms_message_event.processed = 0" +
					" and ebms_message_event.event_type in (" + join(types == null ? EbMSMessageEventType.values() : types,",") + ")" +
					" and ebms_message_event.message_id = ebms_message.message_id" +
					" and ebms_message.message_nr = 0" +
					messageContextFilter +
					" order by ebms_message_event.time_stamp asc" +
					" fetch first " + maxNr + " rows only";
		}
	}
	private static class HSQLDBEbMSMessageEventDAO extends EbMSMessageEventDAOImpl
	{
		public HSQLDBEbMSMessageEventDAO(@NonNull JdbcTemplate jdbcTemplate)
		{
			super(jdbcTemplate);
		}

		@Override
		protected String getMessageEventsQuery(String messageContextFilter, EbMSMessageEventType[] types, int maxNr)
		{
			return "select ebms_message_event.message_id, ebms_message_event.event_type" +
					" from ebms_message_event, ebms_message" +
					" where ebms_message_event.processed = 0" +
					" and ebms_message_event.event_type in (" + join(types == null ? EbMSMessageEventType.values() : types,",") + ")" +
					" and ebms_message_event.message_id = ebms_message.message_id" +
					" and ebms_message.message_nr = 0" +
					messageContextFilter +
					" order by ebms_message_event.time_stamp asc" +
					" limit " + maxNr;
		}
	}
	private static class MSSQLEbMSMessageEventDAO extends EbMSMessageEventDAOImpl
	{
		public MSSQLEbMSMessageEventDAO(@NonNull JdbcTemplate jdbcTemplate)
		{
			super(jdbcTemplate);
		}

		@Override
		protected String getMessageEventsQuery(String messageContextFilter, EbMSMessageEventType[] types, int maxNr)
		{
			return "select top " + maxNr + " ebms_message_event.message_id, ebms_message_event.event_type" +
					" from ebms_message_event, ebms_message" +
					" where ebms_message_event.processed = 0" +
					" and ebms_message_event.event_type in (" + join(types == null ? EbMSMessageEventType.values() : types,",") + ")" +
					" and ebms_message_event.message_id = ebms_message.message_id" +
					" and ebms_message.message_nr = 0" +
					messageContextFilter +
					" order by ebms_message_event.time_stamp asc";
		}
	}
	private static class MySQLEbMSMessageEventDAO extends EbMSMessageEventDAOImpl
	{
		public MySQLEbMSMessageEventDAO(@NonNull JdbcTemplate jdbcTemplate)
		{
			super(jdbcTemplate);
		}

		@Override
		protected String getMessageEventsQuery(String messageContextFilter, EbMSMessageEventType[] types, int maxNr)
		{
			return "select ebms_message_event.message_id, ebms_message_event.event_type" +
					" from ebms_message_event, ebms_message" +
					" where ebms_message_event.processed = 0" +
					" and ebms_message_event.event_type in (" + join(types == null ? EbMSMessageEventType.values() : types,",") + ")" +
					" and ebms_message_event.message_id = ebms_message.message_id" +
					" and ebms_message.message_nr = 0" +
					messageContextFilter +
					" order by ebms_message_event.time_stamp asc" +
					" limit " + maxNr;
		}
	}
	private static class OracleEbMSMessageEventDAO extends EbMSMessageEventDAOImpl
	{
		public OracleEbMSMessageEventDAO(@NonNull JdbcTemplate jdbcTemplate)
		{
			super(jdbcTemplate);
		}

		@Override
		protected String getMessageEventsQuery(String messageContextFilter, EbMSMessageEventType[] types, int maxNr)
		{
			return "select * from (" +
					"select ebms_message_event.message_id, ebms_message_event.event_type" +
					" from ebms_message_event, ebms_message" +
					" where ebms_message_event.processed = 0" +
					" and ebms_message_event.event_type in (" + join(types == null ? EbMSMessageEventType.values() : types,",") + ")" +
					" and ebms_message_event.message_id = ebms_message.message_id" +
					" and ebms_message.message_nr = 0" +
					messageContextFilter +
					" order by ebms_message_event.time_stamp asc)" +
					" where ROWNUM <= " + maxNr;
		}
	}
	private static class PostgreSQLEbMSMessageEventDAO extends EbMSMessageEventDAOImpl
	{
		public PostgreSQLEbMSMessageEventDAO(@NonNull JdbcTemplate jdbcTemplate)
		{
			super(jdbcTemplate);
		}

		@Override
		protected String getMessageEventsQuery(String messageContextFilter, EbMSMessageEventType[] types, int maxNr)
		{
			return "select ebms_message_event.message_id, ebms_message_event.event_type" +
					" from ebms_message_event, ebms_message" +
					" where ebms_message_event.processed = 0" +
					" and ebms_message_event.event_type in (" + join(types == null ? EbMSMessageEventType.values() : types,",") + ")" +
					" and ebms_message_event.message_id = ebms_message.message_id" +
					" and ebms_message.message_nr = 0" +
					messageContextFilter +
					" order by ebms_message_event.time_stamp asc" +
					" limit " + maxNr;
		}
	}
	@NonNull
	JdbcTemplate jdbcTemplate;

	public EbMSMessageEventDAOFactory(DataSource dataSource,	@NonNull JdbcTemplate jdbcTemplate)
	{
		super(dataSource);
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public Class<EbMSMessageEventDAO> getObjectType()
	{
		return EbMSMessageEventDAO.class;
	}

	@Override
	public EbMSMessageEventDAO createDB2DAO()
	{
		return new DB2EbMSMessageEventDAO(jdbcTemplate);
	}

	@Override
	public EbMSMessageEventDAO createH2DAO()
	{
		return new H2EbMSMessageEventDAO(jdbcTemplate);
	}

	@Override
	public EbMSMessageEventDAO createHSQLDBDAO()
	{
		return new HSQLDBEbMSMessageEventDAO(jdbcTemplate);
	}

	@Override
	public EbMSMessageEventDAO createMSSQLDAO()
	{
		return new MSSQLEbMSMessageEventDAO(jdbcTemplate);
	}

	@Override
	public EbMSMessageEventDAO createMySQLDAO()
	{
		return new MySQLEbMSMessageEventDAO(jdbcTemplate);
	}

	@Override
	public EbMSMessageEventDAO createOracleDAO()
	{
		return new OracleEbMSMessageEventDAO(jdbcTemplate);
	}

	@Override
	public EbMSMessageEventDAO createPostgreSQLDAO()
	{
		return new PostgreSQLEbMSMessageEventDAO(jdbcTemplate);
	}
}
