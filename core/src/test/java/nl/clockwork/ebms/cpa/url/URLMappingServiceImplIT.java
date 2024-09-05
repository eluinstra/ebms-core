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
package nl.clockwork.ebms.cpa.url;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import nl.clockwork.ebms.FixedPostgreSQLContainer;
import nl.clockwork.ebms.PropertiesConfig;
import nl.clockwork.ebms.WithFile;
import nl.clockwork.ebms.datasource.DataSourceConfig;
import nl.clockwork.ebms.transaction.TransactionManagerConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// @EnabledIf(expression = "#{systemProperties['spring.profiles.active'] == 'test'}")
@TestInstance(Lifecycle.PER_CLASS)
@Testcontainers
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {PropertiesConfig.class, URLMappingServiceConfig.class, DataSourceConfig.class, TransactionManagerConfig.class})
class URLMappingServiceImplIT implements WithFile
{
	@Container
	static final PostgreSQLContainer<?> database = new FixedPostgreSQLContainer();

	@Autowired
	URLMappingService mappingService;

	@ParameterizedTest
	@MethodSource("invalidURLMappings")
	void insertInvalidXML(URLMapping mapping, String message)
	{
		assertThatThrownBy(() -> mappingService.setURLMapping(mapping)).hasMessageContaining(message);
	}

	static Stream<Arguments> invalidURLMappings()
	{
		return Stream.of(
				// FIXME
				// arguments(new URLMapping(null,null), "source is marked non-null but is null"),
				arguments(new URLMapping("source", "destination"), "Source invalid"));
	}

	@ParameterizedTest
	@MethodSource("validURLMappings")
	void insertValidXML(URLMapping mapping)
	{
		assertThatCode(() -> mappingService.setURLMapping(mapping)).doesNotThrowAnyException();
	}

	static Stream<Arguments> validURLMappings()
	{
		return Stream.of(
				// FIXME
				arguments(new URLMapping("", "")),
				arguments(new URLMapping("http://www.example.com:8080", "http://localhost:8090")),
				arguments(new URLMapping("http://www.example.com:8090", "http://localhost:8090")));
	}

	@Test
	void getURLMappings()
	{
		validURLMappings().forEach(arg -> mappingService.setURLMapping((URLMapping)arg.get()[0]));
		assertThat(mappingService.getURLMappings()).hasSize(2)
				.contains(new URLMapping("http://www.example.com:8080", "http://localhost:8090"))
				.contains(new URLMapping("http://www.example.com:8090", "http://localhost:8090"));
	}

	@Test
	void deleteURLMappings()
	{
		validURLMappings().forEach(arg -> mappingService.setURLMapping((URLMapping)arg.get()[0]));
		assertThat(mappingService.getURLMappings()).hasSize(2);
		assertThatCode(() -> mappingService.deleteURLMapping("http://www.example.com:8080")).doesNotThrowAnyException();
		assertThat(mappingService.getURLMappings()).hasSize(1);
		assertThatThrownBy(() -> mappingService.deleteURLMapping("http://www.example.com:8080")).hasMessageContaining("URL not found");
		assertThatCode(() -> mappingService.deleteURLMapping("http://www.example.com:8090")).doesNotThrowAnyException();
		assertThat(mappingService.getURLMappings()).isEmpty();
	}

	@Test
	void deleteCache()
	{
		assertThatCode(() -> mappingService.deleteCache()).doesNotThrowAnyException();
	}
}
