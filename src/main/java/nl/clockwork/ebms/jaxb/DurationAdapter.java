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
package nl.clockwork.ebms.jaxb;

import java.util.Date;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

public class DurationAdapter extends XmlAdapter<String, java.time.Duration>
{
	private static DatatypeFactory datatypeFactory;

	static
	{
		try
		{
			datatypeFactory = DatatypeFactory.newInstance();
		}
		catch (DatatypeConfigurationException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public java.time.Duration unmarshal(String duration) throws Exception
	{
		return toDuration(datatypeFactory.newDuration(duration));
	}

	@Override
	public String marshal(java.time.Duration duration) throws Exception
	{
		return duration != null ? toDuration(duration).toString() : null;
	}

	private static java.time.Duration toDuration(Duration duration)
	{
		return java.time.Duration.ofMillis(duration.getTimeInMillis(new Date()));
	}

	private static Duration toDuration(java.time.Duration duration)
	{
		return duration != null ? datatypeFactory.newDuration(duration.toString()) : null;
	}
}
