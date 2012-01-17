/*******************************************************************************
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
 ******************************************************************************/
package nl.clockwork.mule.ebms.cxf;

public class CXFHeaderManager
{
	private static ThreadLocal<String> contentType = new ThreadLocal<String>()
	{
		protected synchronized String initialValue()
		{
			return null;
		}
	};

	private static ThreadLocal<Integer> statusCode = new ThreadLocal<Integer>()
	{
		protected synchronized Integer initialValue()
		{
			return null;
		}
	};

	public static void setContentType(String contentType)
	{
		CXFHeaderManager.contentType.set(contentType);
	}

	public static String getContentType()
	{
		return contentType.get();
	}
	
	public static void setStatusCode(Integer statusCode)
	{
		CXFHeaderManager.statusCode.set(statusCode);
	}
	
	public static Integer getStatusCode()
	{
		return statusCode.get();
	}

}
