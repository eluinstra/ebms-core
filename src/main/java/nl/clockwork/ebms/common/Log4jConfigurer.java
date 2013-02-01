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
package nl.clockwork.ebms.common;

import org.apache.log4j.LogManager;
import org.springframework.beans.factory.InitializingBean;

public class Log4jConfigurer implements InitializingBean
{
	private String location;
	private long refreshInterval;

	public Log4jConfigurer(String location)
	{
		this.location = location;
	}

	public Log4jConfigurer(String location, long refreshInterval)
	{
		this.location = location;
		this.refreshInterval = refreshInterval;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		if (location != null)
		{
			LogManager.resetConfiguration();
			if (refreshInterval == 0)
				org.springframework.util.Log4jConfigurer.initLogging(location);
			else
				org.springframework.util.Log4jConfigurer.initLogging(location,refreshInterval);
		}
	}

	public String getLocation()
	{
		return location;
	}

	public void setLocation(String location)
	{
		this.location = location;
	}

	public long getRefreshInterval()
	{
		return refreshInterval;
	}

	public void setRefreshInterval(long refreshInterval)
	{
		this.refreshInterval = refreshInterval;
	}

}
