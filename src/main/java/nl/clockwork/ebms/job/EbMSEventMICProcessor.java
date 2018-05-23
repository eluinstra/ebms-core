/**
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
package nl.clockwork.ebms.job;

import nl.clockwork.ebms.client.EbMSClient;
import nl.clockwork.ebms.client.EbMSHttpMIClientFactory;

public class EbMSEventMICProcessor extends EbMSEventProcessor
{
	private EbMSHttpMIClientFactory ebMSHttpMIClientFactory;

	@Override
	protected EbMSClient getEbMSClient(String clientAlias)
	{
		return ebMSHttpMIClientFactory.getEbMSClient(clientAlias);
	}

	public void setEbMSHttpMIClientFactory(EbMSHttpMIClientFactory ebMSHttpMIClientFactory)
	{
		this.ebMSHttpMIClientFactory = ebMSHttpMIClientFactory;
	}
}
