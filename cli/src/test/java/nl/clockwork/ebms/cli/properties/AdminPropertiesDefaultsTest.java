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
import java.util.Properties;
import lombok.val;
import nl.clockwork.ebms.common.event.MessageEventListenerConfig.EventListenerType;
import nl.clockwork.ebms.common.security.KeyStoreType;
import org.junit.jupiter.api.Test;

class AdminPropertiesDefaultsTest
{
	private AdminProperties read(String content, PropertiesType type) throws Exception
	{
		try (val in = new ByteArrayInputStream(content.getBytes(StandardCharsets.ISO_8859_1)))
		{
			return new AdminPropertiesReader(in, type).read();
		}
	}

	@Test
	void shouldUseDefaultsForEmptyFile() throws Exception
	{
		val model = read("", PropertiesType.EBMS_ADMIN_EMBEDDED);

		assertThat(model.getConsoleProperties().getMaxItemsPerPage()).isEqualTo(20);
		assertThat(model.getCoreProperties().getEventListener()).isEqualTo(EventListenerType.DEFAULT);
		assertThat(model.getHttpProperties().getHost()).isEqualTo("0.0.0.0");
		assertThat(model.getHttpProperties().getPort()).isEqualTo(8088);
		assertThat(model.getHttpProperties().getPath()).isEqualTo("/ebms");
		assertThat(model.getHttpProperties().isChunkedStreamingMode()).isTrue();
		assertThat(model.getServerDatabase().isStart()).isTrue();
		assertThat(model.getServerDatabase().getDir()).isEqualTo("./h2");
		assertThat(model.getJdbcProperties().getDriver()).isEqualTo(JdbcDriver.H2);
		assertThat(model.getJdbcProperties().getUsername()).isEqualTo("sa");
		assertThat(model.getJdbcProperties().getJdbcUrl().getHost()).isEqualTo("localhost");
		assertThat(model.getJdbcProperties().getJdbcUrl().getDatabase()).isEqualTo("ebms");
		assertThat(model.getSignatureProperties().isSigning()).isFalse();
		assertThat(model.getEncryptionProperties().isEncryption()).isFalse();
	}

	@Test
	void shouldParseKeystoreTypeCaseInsensitive() throws Exception
	{
		val content = "ebms.ssl=true\nkeystore.type=jks\nkeystore.path=/keystore.jks\n";
		val model = read(content, PropertiesType.EBMS_ADMIN_EMBEDDED);
		assertThat(model.getHttpProperties().isSsl()).isTrue();
		assertThat(model.getHttpProperties().getSslProperties().getKeystoreProperties().getType()).isEqualTo(KeyStoreType.JKS);
	}

	@Test
	void shouldNotCrashOnUnknownEventListenerTypeAndKeepDefault() throws Exception
	{
		val model = read("eventListener.type=BOGUS\n", PropertiesType.EBMS_ADMIN);
		assertThat(model.getCoreProperties().getEventListener()).isEqualTo(EventListenerType.DEFAULT);
	}

	@Test
	void shouldWriteStableOutputForDefaults() throws Exception
	{
		val model = read("", PropertiesType.EBMS_ADMIN);
		val out = new StringWriter();
		new AdminPropertiesWriter(out).write(model, PropertiesType.EBMS_ADMIN);
		val props = new Properties();
		props.load(new StringReader(out.toString()));
		assertThat(props.getProperty("maxItemsPerPage")).isEqualTo("20");
		assertThat(props.getProperty("eventListener.type")).isEqualTo("DEFAULT");
		assertThat(props.getProperty("service.ebms.url")).isEqualTo("");
		assertThat(props.getProperty("ebms.jdbc.driverClassName")).isEqualTo("org.h2.Driver");
	}
}
