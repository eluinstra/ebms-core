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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.context.DefaultMuleContextFactory;

public class Start
{
	protected static Log logger = LogFactory.getLog(Start.class);
	
	public static void main(String[] args) throws MuleException
	{
		setProperty("ebms.protocol",new String[]{"http","https"},"http");
		DefaultMuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
		SpringXmlConfigurationBuilder configBuilder = new SpringXmlConfigurationBuilder(args.length == 0 ? "main.xml" : args[0]);
		MuleContext muleContext = muleContextFactory.createMuleContext(configBuilder);
		muleContext.start();
	}

	private static void setProperty(String propertyName, String[] expectedValues, String defaultValue)
	{
		String value = System.getProperty(propertyName);
		if (!findValue(expectedValues,value))
		{
			System.getProperties().setProperty(propertyName,"defaultValue");
			logger.info("No valid value set for property " + propertyName + ". Using default value: " + defaultValue);
		}
	}
	
	private static boolean findValue(String[] expectedValues, String value)
	{
		for (String expectedValue : expectedValues)
			if (expectedValue.equals(value))
				return true;
		return false;
	}

}
