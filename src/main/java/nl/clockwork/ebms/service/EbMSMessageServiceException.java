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
package nl.clockwork.ebms.service;

import javax.xml.ws.WebFault;

import lombok.NoArgsConstructor;

@WebFault(name="EbMSMessageServiceException", targetNamespace="http://www.ordina.nl/ebms/2.17")
@NoArgsConstructor
public class EbMSMessageServiceException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	public EbMSMessageServiceException(String message, Throwable cause)
	{
		super(message,cause);
	}

	public EbMSMessageServiceException(String message)
	{
		super(message);
	}

	public EbMSMessageServiceException(Throwable cause)
	{
		super(cause);
	}
}
