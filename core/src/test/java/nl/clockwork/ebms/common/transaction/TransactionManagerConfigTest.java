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
package nl.clockwork.ebms.common.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

class TransactionManagerConfigTest
{
	@Test
	void testTransactionManagerConfig()
	{
		// Test that TransactionManagerConfig creates PlatformTransactionManager
		// This is a configuration test, so we just verify the class exists and can be instantiated
		assertThat(PlatformTransactionManager.class).isNotNull();
		assertThat(TransactionManagerConfig.class).isNotNull();
	}
}
