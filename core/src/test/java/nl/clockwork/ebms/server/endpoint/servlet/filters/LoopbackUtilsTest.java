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
package nl.clockwork.ebms.server.endpoint.servlet.filters;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LoopbackUtilsTest
{
	@ParameterizedTest
	@ValueSource(strings = {"", "   ", "localhost", "LOCALHOST", "127.0.0.1"})
	void isLoopback_returnsTrueForLocalHosts(String host)
	{
		assertThat(LoopbackUtils.isLoopback(host)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = {"0.0.0.0", "192.168.1.10", "10.0.0.1", "172.16.0.1", "8.8.8.8"})
	void isLoopback_returnsFalseForNonLoopbackHosts(String host)
	{
		assertThat(LoopbackUtils.isLoopback(host)).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = {"127.0.0.1", "0.0.0.0", "169.254.1.1", "192.168.1.10", "10.0.0.1", "172.16.0.1"})
	void isPrivateOrLocalHost_returnsTrueForPrivateAndLocalHosts(String host)
	{
		assertThat(LoopbackUtils.isPrivateOrLocalHost(host)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = {"8.8.8.8", "1.1.1.1"})
	void isPrivateOrLocalHost_returnsFalseForPublicHosts(String host)
	{
		assertThat(LoopbackUtils.isPrivateOrLocalHost(host)).isFalse();
	}

	@Test
	void isPrivateOrLocalHost_failsClosedForUnresolvableHosts()
	{
		// A name that cannot be resolved is not a usable public endpoint; the check must fail closed.
		assertThat(LoopbackUtils.isPrivateOrLocalHost("this.host.does.not.resolve.invalid")).isTrue();
	}
}
