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
package nl.clockwork.common.jaxb;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public class EbMSDateTimeConverter
{
	private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

	public static XMLGregorianCalendar parseDateTime(String date)
	{
		try
		{
			date = date.replaceFirst("\\.\\d{0,3}","");
			if (!date.endsWith("Z"))
				date += "Z";
			GregorianCalendar calendar = new GregorianCalendar();
			DateFormat df = new SimpleDateFormat(DATE_FORMAT);
			df.setTimeZone(TimeZone.getTimeZone("GMT"));
			//df.setLenient(true);
			calendar.setTime(df.parse(date));
			return DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
		}
		catch (ParseException e)
		{
			//throw new RuntimeException(e);
			return null;
		}
		catch (DatatypeConfigurationException e)
		{
			//throw new RuntimeException(e);
			return null;
		}
	}

	public static String printDateTime(XMLGregorianCalendar date)
	{
		DateFormat df = new SimpleDateFormat(DATE_FORMAT);
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		//df.setLenient(true);
		return df.format(date.toGregorianCalendar().getTime());
	}
}
