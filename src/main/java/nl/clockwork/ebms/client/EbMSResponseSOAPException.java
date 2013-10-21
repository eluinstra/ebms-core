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
package nl.clockwork.ebms.client;

import javax.xml.namespace.QName;


public class EbMSResponseSOAPException extends EbMSResponseException
{
	public static final QName CLIENT = new QName("http://schemas.xmlsoap.org/soap/envelope/","Client");
	public static final QName SERVER = new QName("http://schemas.xmlsoap.org/soap/envelope/","Server");
	private static final long serialVersionUID = 1L;
	private QName faultCode;

	public EbMSResponseSOAPException(int statusCode, QName faultCode)
	{
		super(statusCode);
		this.faultCode = faultCode;
	}
	
	public EbMSResponseSOAPException(int statusCode, QName faultCode, String message)
	{
		super(statusCode,message);
		this.faultCode = faultCode;
	}
	
	public QName getFaultCode()
	{
		return faultCode;
	}
	
}
