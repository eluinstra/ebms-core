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
package nl.clockwork.mule.ebms;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class DatabaseProvider
{
	protected transient Log logger = LogFactory.getLog(getClass());
	private DataSource dataSource;
	private String driverClassName;
	private boolean execute;
	private String[] sqlFiles;

	public void init() throws SQLException, IOException
	{
		if (execute && driverClassName.equals(((ComboPooledDataSource)dataSource).getDriverClass()))
		{
			try (Connection c = dataSource.getConnection())
			{
				for (String sqlFile : sqlFiles)
				{
					try (Statement s = c.createStatement())
					{
						logger.info("Executing file " + sqlFile);
						s.executeUpdate(IOUtils.toString(DatabaseProvider.class.getResourceAsStream(sqlFile.trim())));
					}
				}
			}
			catch (SQLException e)
			{
				logger.warn("",e);
			}
		}
	}
	
	public void close() throws SQLException
	{
		if (execute)
		{
			try (Connection c = dataSource.getConnection())
			{
				try (Statement s = c.createStatement())
				{
					s.executeUpdate("shutdown");
				}
			}
		}
	}
	
	public void setDataSource(DataSource dataSource)
	{
		this.dataSource = dataSource;
	}
	
	public void setDriverClassName(String driverClassName)
	{
		this.driverClassName = driverClassName;
	}
	
	public void setExecute(boolean execute)
	{
		this.execute = execute;
	}
	
	public void setSqlFiles(String sqlFiles)
	{
		this.sqlFiles = sqlFiles.split(",");
	}
}
