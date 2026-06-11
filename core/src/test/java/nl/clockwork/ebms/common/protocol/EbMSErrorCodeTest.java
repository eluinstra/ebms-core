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

class EbMSErrorCodeTest
{
	@Test
	void testEbMSErrorCodeValues()
	{
		assertThat(EbMSErrorCode.VALUE_NOT_RECOGNIZED.getErrorCode()).isEqualTo("ValueNotRecognized");
		assertThat(EbMSErrorCode.NOT_SUPPORTED.getErrorCode()).isEqualTo("NotSupported");
		assertThat(EbMSErrorCode.INCONSISTENT.getErrorCode()).isEqualTo("Inconsistent");
		assertThat(EbMSErrorCode.OTHER_XML.getErrorCode()).isEqualTo("OtherXml");
		assertThat(EbMSErrorCode.DELIVERY_FAILURE.getErrorCode()).isEqualTo("DeliveryFailure");
		assertThat(EbMSErrorCode.TIME_TO_LIVE_EXPIRED.getErrorCode()).isEqualTo("TimeToLiveExpired");
		assertThat(EbMSErrorCode.SECURITY_FAILURE.getErrorCode()).isEqualTo("SecurityFailure");
		assertThat(EbMSErrorCode.MIME_PROBLEM.getErrorCode()).isEqualTo("MimeProblem");
		assertThat(EbMSErrorCode.UNKNOWN.getErrorCode()).isEqualTo("Unknown");
	}
}
