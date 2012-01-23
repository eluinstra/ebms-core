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
package nl.clockwork.mule.ebms;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.context.DefaultMuleContextFactory;

public class Start
{
	public static void main(String[] args) throws MuleException
	{
		String protocol = System.getProperty("ebms.protocol");
		if (protocol == null || (!"http".equals(protocol) && !"https".equals(protocol)))
		{
			System.getProperties().setProperty("ebms.protocol","http");
			System.out.println("No valid value set for property ebms.protocol. Using default value: http");
		}
		String database = System.getProperty("ebms.database");
		if (database == null || (!"hsqldb".equals(database) && !"mysql".equals(database) && !"postgresql".equals(database) && !"mssql".equals(database) && !"oracle".equals(database)))
		{
			System.getProperties().setProperty("ebms.database","hsqldb");
			System.out.println("No valid value set for property ebms.database. Using default value: hsqldb");
		}
		DefaultMuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
		SpringXmlConfigurationBuilder configBuilder = new SpringXmlConfigurationBuilder(args.length == 0 ? "main.xml" : args[0]);
		MuleContext muleContext = muleContextFactory.createMuleContext(configBuilder);
		muleContext.start();
	}
}
