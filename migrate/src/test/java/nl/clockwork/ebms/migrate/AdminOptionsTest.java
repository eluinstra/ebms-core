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
package nl.clockwork.ebms.migrate;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import lombok.val;
import org.junit.jupiter.api.Test;

class AdminOptionsTest
{
	@Test
	void optionNamesAreUnique()
	{
		val names = AdminOptions.allOptionNames();
		assertThat(names).doesNotHaveDuplicates();
		assertThat(names).hasSizeGreaterThanOrEqualTo(30);
	}

	@Test
	void containsAllDocumented220Options()
	{
		val names = List.of(
				"help",
				"host",
				"port",
				"path",
				"soap",
				"headless",
				"health",
				"healthPort",
				"connectionLimit",
				"queriesPerSecond",
				"userQueriesPerSecond",
				"auditLogging",
				"echoHeaderNames",
				"mdcHeaderNames",
				"ssl",
				"protocols",
				"cipherSuites",
				"keyStoreType",
				"keyStorePath",
				"keyStorePassword",
				"clientAuthentication",
				"clientCertificateHeader",
				"trustStoreType",
				"trustStorePath",
				"trustStorePassword",
				"authentication",
				"clientTrustStoreType",
				"clientTrustStorePath",
				"clientTrustStorePassword",
				"configDir",
				"jmx",
				"jmxPort",
				"jmxAccessFile",
				"jmxPasswordFile");
		assertThat(AdminOptions.allOptionNames()).containsAll(names);
	}
}
