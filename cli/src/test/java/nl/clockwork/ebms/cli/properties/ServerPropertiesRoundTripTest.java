/*
 * Copyright 2011 - 2026 Clockwork
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

class ServerPropertiesRoundTripTest
{
	private ServerProperties read(String content) throws Exception
	{
		try (val in = new ByteArrayInputStream(content.getBytes(StandardCharsets.ISO_8859_1)))
		{
			return new ServerPropertiesReader(in).read();
		}
	}

	private String write(ServerProperties model) throws Exception
	{
		val out = new StringWriter();
		new ServerPropertiesWriter(out).write(model);
		return out.toString();
	}

	private ServerProperties canonical()
	{
		val m = new ServerProperties();
		m.getCoreProperties().setEventListener(EventListenerType.DEFAULT);
		m.getCoreProperties().setJmsBrokerUrl("tcp://localhost:61616");
		m.getCoreProperties().setDeleteMessageContentOnProcessed(true);
		val http = m.getHttpProperties();
		http.setHost("0.0.0.0");
		http.setPort(8888);
		http.setPath("/ebms");
		http.setChunkedStreamingMode(true);
		http.setSsl(false);
		m.getServerDatabase().setStart(true);
		m.getServerDatabase().setDir("./h2");
		m.getJdbcProperties().setDriver(JdbcDriver.POSTGRESQL);
		m.getJdbcProperties().getJdbcUrl().setHost("db.example.com");
		m.getJdbcProperties().getJdbcUrl().setPort(5432);
		m.getJdbcProperties().getJdbcUrl().setDatabase("ebms");
		m.getJdbcProperties().setUsername("ebms");
		m.getJdbcProperties().setPassword("secret");
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
	void shouldRoundTrip() throws Exception
	{
		val model = canonical();
		val readBack = read(write(model));

		assertThat(readBack.getCoreProperties().getEventListener()).isEqualTo(EventListenerType.DEFAULT);
		assertThat(readBack.getCoreProperties().getJmsBrokerUrl()).isEqualTo("tcp://localhost:61616");
		assertThat(readBack.getCoreProperties().isDeleteMessageContentOnProcessed()).isTrue();
		assertThat(readBack.getHttpProperties().getHost()).isEqualTo("0.0.0.0");
		assertThat(readBack.getHttpProperties().getPort()).isEqualTo(8888);
		assertThat(readBack.getHttpProperties().getPath()).isEqualTo("/ebms");
		assertThat(readBack.getHttpProperties().isChunkedStreamingMode()).isTrue();
		assertThat(readBack.getHttpProperties().isSsl()).isFalse();
		assertThat(readBack.getServerDatabase().isStart()).isTrue();
		assertThat(readBack.getServerDatabase().getDir()).isEqualTo("./h2");
		assertThat(readBack.getJdbcProperties().getDriver()).isEqualTo(JdbcDriver.POSTGRESQL);
		assertThat(readBack.getJdbcProperties().getJdbcUrl().getHost()).isEqualTo("db.example.com");
		assertThat(readBack.getJdbcProperties().getJdbcUrl().getPort()).isEqualTo(5432);
		assertThat(readBack.getJdbcProperties().getJdbcUrl().getDatabase()).isEqualTo("ebms");
		assertThat(readBack.getJdbcProperties().getUrl()).isEqualTo("jdbc:postgresql://db.example.com:5432/ebms");
		assertThat(readBack.getJdbcProperties().getUsername()).isEqualTo("ebms");
		assertThat(readBack.getJdbcProperties().getPassword()).isEqualTo("secret");
		assertThat(readBack.getSignatureProperties().isSigning()).isTrue();
		assertThat(readBack.getSignatureProperties().getKeystoreProperties().getType()).isEqualTo(KeyStoreType.PKCS12);
		assertThat(readBack.getSignatureProperties().getKeystoreProperties().getUri()).isEqualTo("file:/etc/ebms/signing.p12");
		assertThat(readBack.getSignatureProperties().getKeystoreProperties().getPassword()).isEqualTo("sigpass");
		assertThat(readBack.getEncryptionProperties().isEncryption()).isTrue();
		assertThat(readBack.getEncryptionProperties().getKeystoreProperties().getUri()).isEqualTo("file:/etc/ebms/encryption.p12");
		assertThat(readBack.getEncryptionProperties().getKeystoreProperties().getPassword()).isEqualTo("encpass");
	}

	@Test
	void shouldWriteJdbc() throws Exception
	{
		// the JDBC section must survive a round trip (regression: it used to be dropped)
		val props = toProperties(write(canonical()));
		assertThat(props.getProperty("ebms.jdbc.driverClassName")).isEqualTo("org.postgresql.Driver");
		assertThat(props.getProperty("ebms.jdbc.url")).isEqualTo("jdbc:postgresql://db.example.com:5432/ebms");
	}

	@Test
	void shouldWriteSslOnlyWhenEnabled() throws Exception
	{
		val model = canonical();
		val withoutSsl = write(model);
		assertThat(toProperties(withoutSsl).getProperty("keystore.path")).isNull();

		model.getHttpProperties().setSsl(true);
		model.getHttpProperties().getSslProperties().getKeystoreProperties().setUri("file:/etc/ebms/server.p12");
		model.getHttpProperties().getSslProperties().setEnabledProtocols(List.of("TLSv1.2", "TLSv1.3"));
		val sslContent = write(model);
		val props = toProperties(sslContent);
		assertThat(props.getProperty("keystore.path")).isEqualTo("file:/etc/ebms/server.p12");
		assertThat(props.getProperty("https.protocols")).isEqualTo("TLSv1.2,TLSv1.3");

		val readBack = read(sslContent);
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
