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
package nl.clockwork.ebms.jaxb;

import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.bind.DatatypeConverter;

public class EbMSDateTimeConverter
{
	public static Date parseDateTime(String date)
	{
		return DatatypeConverter.parseDateTime(date).getTime();
	}

	public static String printDateTime(Date date)
	{
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		return DatatypeConverter.printDateTime(calendar);
	}
}
