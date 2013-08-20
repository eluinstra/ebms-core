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
package nl.clockwork.ebms.dao;

import java.sql.Connection;

public class ConnectionManager
{
	private static class MyConnection
	{
		public Connection connection;
		public int nr;
	}
	private static final ThreadLocal<MyConnection> connectionHolder =
		new ThreadLocal<MyConnection>()
		{
			protected MyConnection initialValue()
			{
				return new MyConnection();
			};
		};
	
	public static void set(Connection connection)
	{
		if (connectionHolder.get().nr == 0)
			connectionHolder.get().connection = connection;
		connectionHolder.get().nr++;
	}
	
	public static void unset()
	{
		connectionHolder.get().nr--;
		if (connectionHolder.get().nr == 0)
			connectionHolder.remove();
	}
	
	public static Connection get()
	{
		return connectionHolder.get().connection;
	}

	public static boolean isSet()
	{
		return connectionHolder.get().connection != null;
	}

	public static boolean commit()
	{
		return connectionHolder.get().nr == 1;
	}

	public static boolean close()
	{
		return connectionHolder.get().nr == 1;
	}

}
