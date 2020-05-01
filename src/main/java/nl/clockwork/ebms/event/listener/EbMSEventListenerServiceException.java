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
package nl.clockwork.ebms.event.listener;

import javax.xml.ws.WebFault;

import lombok.NoArgsConstructor;

@WebFault(name="EbMSEventListenerServiceException", targetNamespace="http://www.clockwork.nl/ebms/event/2.17")
@NoArgsConstructor
public class EbMSEventListenerServiceException extends EventException
{
	private static final long serialVersionUID = 1L;

	public EbMSEventListenerServiceException(String message, Throwable cause)
	{
		super(message,cause);
	}

	public EbMSEventListenerServiceException(String message)
	{
		super(message);
	}

	public EbMSEventListenerServiceException(Throwable cause)
	{
		super(cause);
	}
}
