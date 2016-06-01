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
package nl.clockwork.ebms.common.util;

import java.util.List;
import java.util.Map;

import org.springframework.util.StringUtils;


public class HTTPUtils
{

	public static String getCharSet(String contentType)
	{
		String charset = null;
		for (String param: contentType.replace(" ","").split(";"))
		{
			if (param.startsWith("charset="))
			{
				charset = param.split("=",2)[1];
				break;
			}
		}
		return charset;
	}

	public static String toString(Map<String,List<String>> properties)
	{
		String result = "";
		for (String key : properties.keySet())
			result += (key != null ? key + ": " : "") + StringUtils.collectionToCommaDelimitedString(properties.get(key)) + "\n";
		return result;
	}
}
