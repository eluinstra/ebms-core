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
package nl.clockwork.ebms.datasource;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static nl.clockwork.ebms.Predicates.contains;

import java.beans.PropertyVetoException;
import java.sql.Types;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.atomikos.jdbc.internal.AtomikosSQLException;
import com.querydsl.sql.DB2Templates;
import com.querydsl.sql.HSQLDBTemplates;
import com.querydsl.sql.MySQLTemplates;
import com.querydsl.sql.OracleTemplates;
import com.querydsl.sql.PostgreSQLTemplates;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.SQLServer2012Templates;
import com.querydsl.sql.SQLTemplates;
import com.querydsl.sql.spring.SpringConnectionProvider;
import com.querydsl.sql.spring.SpringExceptionTranslator;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.dao.AbstractDAOFactory;
import nl.clockwork.ebms.querydsl.CachedOutputStreamType;
import nl.clockwork.ebms.querydsl.CollaborationProtocolAgreementType;
import nl.clockwork.ebms.querydsl.DocumentType;
import nl.clockwork.ebms.querydsl.EbMSEventStatusType;
import nl.clockwork.ebms.querydsl.EbMSMessageEventTypeType;
import nl.clockwork.ebms.querydsl.EbMSMessageStatusType;
import nl.clockwork.ebms.querydsl.InstantType;
import nl.clockwork.ebms.querydsl.X509CertificateType;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QueryDSLConfig
{
	@Autowired
	DataSource dataSource;
	
	@Bean
	public SQLQueryFactory queryFactory() throws AtomikosSQLException, PropertyVetoException
	{
		val provider = new SpringConnectionProvider(dataSource);
		return new SQLQueryFactory(querydslConfiguration(),provider);
	}

	@Bean
	public com.querydsl.sql.Configuration querydslConfiguration() throws AtomikosSQLException, PropertyVetoException
	{
		val templates = getSQLTemplates();
		val configuration = new com.querydsl.sql.Configuration(templates);
		configuration.setExceptionTranslator(new SpringExceptionTranslator());
		configuration.register(new InstantType(Types.TIMESTAMP));
		configuration.register("cpa","cpa",new CollaborationProtocolAgreementType(Types.CLOB));
		configuration.register("certificate_mapping","source",new X509CertificateType(Types.BLOB));
		configuration.register("certificate_mapping","destination",new X509CertificateType(Types.BLOB));
		configuration.register("ebms_message_event","event_type",new EbMSMessageEventTypeType(Types.SMALLINT));
		configuration.register("ebms_event_log","status",new EbMSEventStatusType(Types.SMALLINT));
		configuration.register("ebms_message","content",new DocumentType(Types.CLOB));
		configuration.register("ebms_message","status",new EbMSMessageStatusType(Types.SMALLINT));
		configuration.register("ebms_attachment","content",new CachedOutputStreamType(Types.BLOB));
		return configuration;
	}

	private SQLTemplates getSQLTemplates() throws AtomikosSQLException, PropertyVetoException
	{
		return createSQLTemplates(dataSource);
	}

	private SQLTemplates createSQLTemplates(DataSource dataSource) throws AtomikosSQLException, PropertyVetoException
	{
		String jdbcUrl = AbstractDAOFactory.getDriverClassName(dataSource);
		return Match(jdbcUrl).of(
				Case($(contains("db2")),o -> DB2Templates.builder().build()),
				Case($(contains("hsqldb")),o -> HSQLDBTemplates.builder().build()),
				Case($(contains("mysql")),o -> MySQLTemplates.builder().build()),
				Case($(contains("oracle")),o -> OracleTemplates.builder().build()),
				Case($(contains("postgresql")),o -> PostgreSQLTemplates.builder().build()),
				Case($(contains("sqlserver")),o -> SQLServer2012Templates.builder().build()),
				Case($(),o -> {
					throw new RuntimeException("Jdbc url " + jdbcUrl + " not recognized!");
				}));
	}
}
