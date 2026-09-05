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
import org.junit.jupiter.api.Test;

class AdminPropertiesPreserveUnknownTest
{
	private final String embeddedContent = """
			database.start=true
			ebms.jdbc.driverClassName=org.h2.Driver
			ebms.jdbc.url=jdbc:h2:tcp://localhost:9092/ebms
			ebms.jdbc.username=sa
			ebms.jdbc.password=secret
			# a custom key that is not part of the model
			custom.feature.enabled=true
			my.extra.key=value with spaces
			""";

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

	@Test
	void shouldCollectUnmodeledPropertiesAsAdditional() throws Exception
	{
		val model = read(embeddedContent, PropertiesType.EBMS_ADMIN_EMBEDDED);
		assertThat(model.getAdditionalProperties()).containsEntry("custom.feature.enabled", "true").containsEntry("my.extra.key", "value with spaces");
	}

	@Test
	void shouldNotCollectModeledPropertiesAsAdditional() throws Exception
	{
		val model = read(embeddedContent, PropertiesType.EBMS_ADMIN_EMBEDDED);
		assertThat(model.getAdditionalProperties())
				.doesNotContainKeys("database.start", "ebms.jdbc.driverClassName", "ebms.jdbc.url", "ebms.jdbc.username", "ebms.jdbc.password");
	}

	@Test
	void shouldPreserveUnmodeledPropertiesOnWrite() throws Exception
	{
		val model = read(embeddedContent, PropertiesType.EBMS_ADMIN_EMBEDDED);
		// the user edits a modeled value
		model.getJdbcProperties().setPassword("changed");
		val props = toProperties(write(model, PropertiesType.EBMS_ADMIN_EMBEDDED));
		assertThat(props.getProperty("custom.feature.enabled")).isEqualTo("true");
		assertThat(props.getProperty("my.extra.key")).isEqualTo("value with spaces");
		assertThat(props.getProperty("ebms.jdbc.password")).isEqualTo("changed");
	}

	@Test
	void shouldKeepUnmodeledPropertyWhenItMatchesACuratedSection() throws Exception
	{
		// a key that the user changed is written from the model, but still kept in the output
		val model = read(embeddedContent, PropertiesType.EBMS_ADMIN_EMBEDDED);
		assertThat(model.getAdditionalProperties()).hasSize(2);
		// after a full round trip the same two unmodeled keys remain
		val props = toProperties(write(model, PropertiesType.EBMS_ADMIN_EMBEDDED));
		assertThat(props).containsKeys("custom.feature.enabled", "my.extra.key");
	}

	private static Properties toProperties(String content) throws Exception
	{
		val props = new Properties();
		props.load(new StringReader(content));
		return props;
	}
}
