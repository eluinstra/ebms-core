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
	private static final ThreadLocal<ConnectionManager> threadLocal =
		new ThreadLocal<ConnectionManager>()
		{
			protected ConnectionManager initialValue()
			{
				return new ConnectionManager();
			};
		};
	
	public Connection connection;
	public int transactionCount = 0;

	public static void set(Connection connection)
	{
		if (threadLocal.get().transactionCount == 0)
			threadLocal.get().connection = connection;
		threadLocal.get().transactionCount++;
	}
	
	public static void unset()
	{
		threadLocal.get().transactionCount--;
		if (threadLocal.get().transactionCount == 0)
			threadLocal.remove();
	}
	
	public static Connection get()
	{
		return threadLocal.get().connection;
	}

	public static boolean isSet()
	{
		return threadLocal.get().connection != null;
	}

	public static boolean commit()
	{
		return threadLocal.get().transactionCount == 1;
	}

	public static boolean close()
	{
		return threadLocal.get().transactionCount == 1;
	}

}
