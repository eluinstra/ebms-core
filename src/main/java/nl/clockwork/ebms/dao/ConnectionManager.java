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
	private static final ThreadLocal<Connection> connection =  new ThreadLocal<Connection>();
	
	public static void set(Connection connection)
	{
		ConnectionManager.connection.set(connection);
	}
	
	public static void unset()
	{
		connection.remove();
	}
	
	public static Connection get()
	{
		return connection.get();
	}

}
