/*
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
package nl.clockwork.ebms.cli.properties;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import lombok.val;
import nl.clockwork.ebms.common.event.MessageEventListenerConfig.EventListenerType;
import nl.clockwork.ebms.common.security.KeyStoreType;
import org.junit.jupiter.api.Test;

class AdminPropertiesRoundTripTest
{
	private AdminProperties read(String content, PropertiesType type) throws Exception
	{
		try (val in = new ByteArrayInputStream(content.getBytes(StandardCharsets.ISO_8859_1)))
		{
			return new AdminPropertiesReader(in, type).read();
		}
	}

	private String write(AdminProperties model, PropertiesType type) throws Exception
	{
		val out = new StringWriter();
		new AdminPropertiesWriter(out).write(model, type);
		return out.toString();
	}

	private AdminProperties canonicalAdmin()
	{
		val m = new AdminProperties();
		m.getConsoleProperties().setMaxItemsPerPage(25);
		m.getCoreProperties().setEventListener(EventListenerType.DEFAULT);
		m.getCoreProperties().setJmsBrokerUrl("tcp://localhost:61616");
		m.getCoreProperties().setDeleteMessageContentOnProcessed(true);
		m.getServiceProperties().setUrl("http://localhost:8088/ebms");
		m.getJdbcProperties().setDriver(JdbcDriver.POSTGRESQL);
		m.getJdbcProperties().getJdbcUrl().setHost("db.example.com");
		m.getJdbcProperties().getJdbcUrl().setPort(5432);
		m.getJdbcProperties().getJdbcUrl().setDatabase("ebms");
		m.getJdbcProperties().setUsername("ebms");
		m.getJdbcProperties().setPassword("secret");
		return m;
	}

	private AdminProperties canonicalEmbedded()
	{
		val m = canonicalAdmin();
		val http = m.getHttpProperties();
		http.setHost("0.0.0.0");
		http.setPort(8088);
		http.setPath("/ebms");
		http.setChunkedStreamingMode(true);
		http.setSsl(false);
		m.getServerDatabase().setStart(true);
		m.getServerDatabase().setDir("./h2");
		m.getSignatureProperties().setSigning(true);
		m.getSignatureProperties().getKeystoreProperties().setType(KeyStoreType.PKCS12);
		m.getSignatureProperties().getKeystoreProperties().setUri("file:/etc/ebms/signing.p12");
		m.getSignatureProperties().getKeystoreProperties().setPassword("sigpass");
		m.getEncryptionProperties().setEncryption(true);
		m.getEncryptionProperties().getKeystoreProperties().setType(KeyStoreType.PKCS12);
		m.getEncryptionProperties().getKeystoreProperties().setUri("file:/etc/ebms/encryption.p12");
		m.getEncryptionProperties().getKeystoreProperties().setPassword("encpass");
		return m;
	}

	@Test
	void shouldRoundTripAdmin() throws Exception
	{
		val model = canonicalAdmin();
		val readBack = read(write(model, PropertiesType.EBMS_ADMIN), PropertiesType.EBMS_ADMIN);

		assertThat(readBack.getConsoleProperties().getMaxItemsPerPage()).isEqualTo(25);
		assertThat(readBack.getCoreProperties().getEventListener()).isEqualTo(EventListenerType.DEFAULT);
		assertThat(readBack.getCoreProperties().getJmsBrokerUrl()).isEqualTo("tcp://localhost:61616");
		assertThat(readBack.getCoreProperties().isDeleteMessageContentOnProcessed()).isTrue();
		assertThat(readBack.getServiceProperties().getUrl()).isEqualTo("http://localhost:8088/ebms");
		assertThat(readBack.getJdbcProperties().getDriver()).isEqualTo(JdbcDriver.POSTGRESQL);
		assertThat(readBack.getJdbcProperties().getJdbcUrl().getHost()).isEqualTo("db.example.com");
		assertThat(readBack.getJdbcProperties().getJdbcUrl().getPort()).isEqualTo(5432);
		assertThat(readBack.getJdbcProperties().getJdbcUrl().getDatabase()).isEqualTo("ebms");
		assertThat(readBack.getJdbcProperties().getUrl()).isEqualTo("jdbc:postgresql://db.example.com:5432/ebms");
		assertThat(readBack.getJdbcProperties().getUsername()).isEqualTo("ebms");
		assertThat(readBack.getJdbcProperties().getPassword()).isEqualTo("secret");
	}

	@Test
	void shouldRoundTripEmbedded() throws Exception
	{
		val model = canonicalEmbedded();
		val readBack = read(write(model, PropertiesType.EBMS_ADMIN_EMBEDDED), PropertiesType.EBMS_ADMIN_EMBEDDED);

		assertThat(readBack.getHttpProperties().getHost()).isEqualTo("0.0.0.0");
		assertThat(readBack.getHttpProperties().getPort()).isEqualTo(8088);
		assertThat(readBack.getHttpProperties().getPath()).isEqualTo("/ebms");
		assertThat(readBack.getHttpProperties().isChunkedStreamingMode()).isTrue();
		assertThat(readBack.getHttpProperties().isSsl()).isFalse();
		assertThat(readBack.getServerDatabase().isStart()).isTrue();
		assertThat(readBack.getServerDatabase().getDir()).isEqualTo("./h2");
		assertThat(readBack.getSignatureProperties().isSigning()).isTrue();
		assertThat(readBack.getSignatureProperties().getKeystoreProperties().getType()).isEqualTo(KeyStoreType.PKCS12);
		assertThat(readBack.getSignatureProperties().getKeystoreProperties().getUri()).isEqualTo("file:/etc/ebms/signing.p12");
		assertThat(readBack.getSignatureProperties().getKeystoreProperties().getPassword()).isEqualTo("sigpass");
		assertThat(readBack.getEncryptionProperties().isEncryption()).isTrue();
		assertThat(readBack.getEncryptionProperties().getKeystoreProperties().getUri()).isEqualTo("file:/etc/ebms/encryption.p12");
		assertThat(readBack.getEncryptionProperties().getKeystoreProperties().getPassword()).isEqualTo("encpass");
	}

	@Test
	void shouldOnlyWriteJdbcForBothTypes() throws Exception
	{
		// the JDBC section must survive a non-embedded round trip (regression: it used to be dropped)
		val content = write(canonicalAdmin(), PropertiesType.EBMS_ADMIN);
		val props = toProperties(content);
		assertThat(props.getProperty("ebms.jdbc.driverClassName")).isEqualTo("org.postgresql.Driver");
		assertThat(props.getProperty("ebms.jdbc.url")).isEqualTo("jdbc:postgresql://db.example.com:5432/ebms");
	}

	@Test
	void shouldNotWriteHttpForAdmin() throws Exception
	{
		val props = toProperties(write(canonicalAdmin(), PropertiesType.EBMS_ADMIN));
		assertThat(props.getProperty("ebms.host")).isNull();
		assertThat(props.getProperty("database.start")).isNull();
	}

	@Test
	void shouldNotWriteServiceForEmbedded() throws Exception
	{
		val props = toProperties(write(canonicalEmbedded(), PropertiesType.EBMS_ADMIN_EMBEDDED));
		assertThat(props.getProperty("service.ebms.url")).isNull();
	}

	@Test
	void shouldWriteSslOnlyWhenEnabled() throws Exception
	{
		val model = canonicalEmbedded();
		val withSsl = write(model, PropertiesType.EBMS_ADMIN_EMBEDDED);
		assertThat(toProperties(withSsl).getProperty("keystore.path")).isNull();

		model.getHttpProperties().setSsl(true);
		model.getHttpProperties().getSslProperties().getKeystoreProperties().setUri("file:/etc/ebms/server.p12");
		model.getHttpProperties().getSslProperties().setEnabledProtocols(List.of("TLSv1.2", "TLSv1.3"));
		val sslContent = write(model, PropertiesType.EBMS_ADMIN_EMBEDDED);
		val props = toProperties(sslContent);
		assertThat(props.getProperty("keystore.path")).isEqualTo("file:/etc/ebms/server.p12");
		assertThat(props.getProperty("https.protocols")).isEqualTo("TLSv1.2,TLSv1.3");

		val readBack = read(sslContent, PropertiesType.EBMS_ADMIN_EMBEDDED);
		assertThat(readBack.getHttpProperties().isSsl()).isTrue();
		assertThat(readBack.getHttpProperties().getSslProperties().getProtocols()).isEqualTo("TLSv1.2,TLSv1.3");
		assertThat(readBack.getHttpProperties().getSslProperties().getKeystoreProperties().getUri()).isEqualTo("file:/etc/ebms/server.p12");
	}

	private static Properties toProperties(String content) throws Exception
	{
		val props = new Properties();
		props.load(new StringReader(content));
		return props;
	}
}
