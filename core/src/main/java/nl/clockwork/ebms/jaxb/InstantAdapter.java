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

import jakarta.xml.bind.DatatypeConverter;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import java.time.Instant;
import java.util.Date;
import java.util.GregorianCalendar;
import lombok.val;

public class InstantAdapter extends XmlAdapter<String, Instant>
{
	@Override
	public Instant unmarshal(String v) throws Exception
	{
		return DatatypeConverter.parseDateTime(v).getTime().toInstant();
	}

	@Override
	public String marshal(Instant v) throws Exception
	{
		val calendar = new GregorianCalendar();
		calendar.setTime(Date.from(v));
		return DatatypeConverter.printDateTime(calendar);
	}
}
