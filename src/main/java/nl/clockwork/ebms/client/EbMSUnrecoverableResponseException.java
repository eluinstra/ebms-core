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

import java.util.List;
import java.util.Map;

public class EbMSUnrecoverableResponseException extends EbMSResponseException
{
	private static final long serialVersionUID = 1L;

	public EbMSUnrecoverableResponseException(int statusCode, Map<String,List<String>> headers)
	{
		super(statusCode,headers);
	}
	
	public EbMSUnrecoverableResponseException(int statusCode, Map<String,List<String>> headers, String message)
	{
		super(statusCode,headers,message);
	}
	
}
