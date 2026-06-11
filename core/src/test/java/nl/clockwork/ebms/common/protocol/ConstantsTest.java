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
package nl.clockwork.ebms.common.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConstantsTest
{
	@Test
	void testConstants()
	{
		assertThat(Constants.EBMS_SOAP_ACTION).isEqualTo("\"ebXML\"");
		assertThat(Constants.EBMS_VERSION).isEqualTo("2.0");
		assertThat(Constants.EBMS_DEFAULT_LANGUAGE).isEqualTo("en-US");
		assertThat(Constants.NSURI_SOAP_ENVELOPE).isEqualTo("http://schemas.xmlsoap.org/soap/envelope/");
		assertThat(Constants.NSURI_SOAP_NEXT_ACTOR).isEqualTo("http://schemas.xmlsoap.org/soap/actor/next");
		assertThat(Constants.CID).isEqualTo("cid:");
		assertThat(Constants.MINUTE_IN_MILLIS).isEqualTo(60000);
		assertThat(Constants.MESSAGE_LOG).isEqualTo("nl.clockwork.ebms.message");
	}
}
