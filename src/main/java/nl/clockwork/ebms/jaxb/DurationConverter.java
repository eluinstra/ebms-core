package nl.clockwork.ebms.jaxb;

import java.util.Date;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

public class DurationConverter
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

	public static java.time.Duration parseDuration(String duration)
	{
		return toDuration(datatypeFactory.newDuration(duration));
	}

	public static String printDuration(java.time.Duration duration)
	{
		return duration != null ? toDuration(duration).toString() : null;
	}

	private static java.time.Duration toDuration(Duration duration)
	{
		return java.time.Duration.ofMillis(duration.getTimeInMillis(new Date()));
	}
	
	private static Duration toDuration(java.time.Duration duration)
	{
		return duration != null ? datatypeFactory.newDuration(duration.toMillis()) : null;
	}
}
