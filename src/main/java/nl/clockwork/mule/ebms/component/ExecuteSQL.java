package nl.clockwork.mule.ebms.component;

import java.sql.Connection;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Callable;

public class ExecuteSQL implements Callable
{
	protected transient Log logger = LogFactory.getLog(getClass());
	private DataSource dataSource;

	@Override
	public Object onCall(MuleEventContext eventContext) throws Exception
	{
		MuleMessage message = eventContext.getMessage();
		if (message.getPayload() instanceof String)
		{
			Connection c = dataSource.getConnection();
			try
			{
				Statement s = c.createStatement();
				s.executeUpdate((String)message.getPayload());
				s.close();
			}
			finally
			{
				c.close();
			}
		}
		return message;
	}

	public void setDataSource(DataSource dataSource)
	{
		this.dataSource = dataSource;
	}

}
