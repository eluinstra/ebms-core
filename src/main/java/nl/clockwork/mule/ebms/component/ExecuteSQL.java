package nl.clockwork.mule.ebms.component;

import java.sql.Connection;
import java.sql.Statement;

import javax.sql.DataSource;

import nl.clockwork.mule.common.component.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;

public class ExecuteSQL  extends Callable
{
	protected transient Log logger = LogFactory.getLog(getClass());
	private DataSource dataSource;

	@Override
	public Object onCall(MuleMessage message) throws Exception
	{
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
