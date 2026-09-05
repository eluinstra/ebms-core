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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PropertiesTypeTest
{
	@Test
	void shouldFindTypeByFileName()
	{
		assertThat(PropertiesType.getPropertiesType("ebms-admin.properties")).contains(PropertiesType.EBMS_ADMIN);
		assertThat(PropertiesType.getPropertiesType("ebms-admin.embedded.properties")).contains(PropertiesType.EBMS_ADMIN_EMBEDDED);
		assertThat(PropertiesType.getPropertiesType("other.properties")).isEmpty();
	}

	@Test
	void shouldFailOnUnknownType()
	{
		assertThatThrownBy(() -> PropertiesType.getPropertiesTypeOrFail("unknown.properties")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldExposePropertiesFileName()
	{
		assertThat(PropertiesType.EBMS_ADMIN.getPropertiesFile()).isEqualTo("ebms-admin.properties");
		assertThat(PropertiesType.EBMS_ADMIN_EMBEDDED.getPropertiesFile()).isEqualTo("ebms-admin.embedded.properties");
	}
}
