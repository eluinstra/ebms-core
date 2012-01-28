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
package nl.clockwork.common;

import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;

public class SimpleLog4jConfigurer
{
	protected transient Log logger = LogFactory.getLog(getClass());
	 
	public SimpleLog4jConfigurer(String location)
	{
		this(location,0);
	}

	public SimpleLog4jConfigurer(String location, long refreshInterval)
	{
		resetConfiguration(location,refreshInterval);
	}
	
	public void resetConfiguration(String location, long refreshInterval)
	{
		URL url = this.getClass().getResource(location);
		if (url != null)
		{
			LogManager.getRootLogger().removeAllAppenders();
			LogManager.resetConfiguration();
			if (location.toLowerCase().endsWith(".xml"))
				if (refreshInterval == 0)
					DOMConfigurator.configure(url);
				else
					DOMConfigurator.configureAndWatch(location,refreshInterval);
			else
				if (refreshInterval == 0)
					PropertyConfigurator.configure(url);
				else
					PropertyConfigurator.configureAndWatch(location,refreshInterval);
		}
		else
		{
			logger.error("Could not find the following file: " + location);
		}
	}
	
}
