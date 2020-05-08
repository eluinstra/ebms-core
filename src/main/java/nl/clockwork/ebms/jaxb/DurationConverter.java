package nl.clockwork.ebms.jaxb;

import java.util.Date;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

public class DurationConverter
{
	private static DatatypeFactory datatypeFactory;
	
	public DurationConverter() throws DatatypeConfigurationException
	{
		datatypeFactory = DatatypeFactory.newInstance();
	}

	public static java.time.Duration parseDuration(String duration)
	{
		return toDuration(datatypeFactory.newDuration(duration));
	}

	public static String printDuration(java.time.Duration duration)
	{
		return toDuration(duration).toString();
	}

	private static java.time.Duration toDuration(Duration duration)
	{
		return java.time.Duration.ofMillis(duration.getTimeInMillis(new Date()));
	}
	
	private static Duration toDuration(java.time.Duration duration)
	{
		return datatypeFactory.newDuration(duration.toMillis());
	}
}
