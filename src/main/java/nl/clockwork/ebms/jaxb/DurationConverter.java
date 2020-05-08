package nl.clockwork.ebms.jaxb;

import java.util.Date;

import javax.xml.datatype.Duration;

public class DurationConverter
{
	public static java.time.Duration toDuration(Duration duration)
	{
		return java.time.Duration.ofMillis(duration.getTimeInMillis(new Date()));
	}
}
