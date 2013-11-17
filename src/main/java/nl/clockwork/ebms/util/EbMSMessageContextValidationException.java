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
package nl.clockwork.ebms.util;

import javax.xml.ws.WebFault;


@WebFault(name="EbMSMessageServiceException", targetNamespace="http://www.clockwork.nl/ebms/1.2")
public class EbMSMessageContextValidationException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	public EbMSMessageContextValidationException()
	{
		super();
	}

	public EbMSMessageContextValidationException(String message, Throwable cause)
	{
		super(message,cause);
	}

	public EbMSMessageContextValidationException(String message)
	{
		super(message);
	}

	public EbMSMessageContextValidationException(Throwable cause)
	{
		super(cause);
	}
}
