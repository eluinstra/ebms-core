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

import lombok.val;
import org.junit.jupiter.api.Test;

class JdbcUrlTest
{
	@Test
	void shouldParseH2TcpUrlWithPort()
	{
		val url = JdbcUrl.parse("jdbc:h2:tcp://localhost:9092/ebms");
		assertThat(url).isPresent();
		assertThat(url.get().getHost()).isEqualTo("localhost");
		assertThat(url.get().getPort()).isEqualTo(9092);
		assertThat(url.get().getDatabase()).isEqualTo("ebms");
	}

	@Test
	void shouldParsePostgresUrlWithPort()
	{
		val url = JdbcUrl.parse("jdbc:postgresql://db.example.com:5432/ebms");
		assertThat(url).isPresent();
		assertThat(url.get().getHost()).isEqualTo("db.example.com");
		assertThat(url.get().getPort()).isEqualTo(5432);
		assertThat(url.get().getDatabase()).isEqualTo("ebms");
	}

	@Test
	void shouldParseUrlWithoutPort()
	{
		val url = JdbcUrl.parse("jdbc:postgresql://db.example.com/ebms");
		assertThat(url).isPresent();
		assertThat(url.get().getHost()).isEqualTo("db.example.com");
		assertThat(url.get().getPort()).isNull();
		assertThat(url.get().getDatabase()).isEqualTo("ebms");
	}

	@Test
	void shouldReturnEmptyForBlankUrl()
	{
		assertThat(JdbcUrl.parse("")).isEmpty();
		assertThat(JdbcUrl.parse(null)).isEmpty();
	}

	@Test
	void shouldReturnEmptyForUrlWithoutNetworkPart()
	{
		// in-memory urls have no host part, so host/port/database are left to the caller's defaults
		assertThat(JdbcUrl.parse("jdbc:h2:mem:ebms")).isEmpty();
	}
}
