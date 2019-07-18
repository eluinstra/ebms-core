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
package nl.clockwork.ebms.job;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class JobScheduler implements InitializingBean, DisposableBean
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private Timer jobTimer = null;
	private boolean enabled;
	private long delay;
	private long period;
	private List<Job> jobs = new ArrayList<Job>();

	@Override
	public void afterPropertiesSet() throws Exception
	{
		if (enabled)
		{
			jobTimer = new Timer();
			jobs.stream().forEach(j ->
				jobTimer.schedule(new TimerTask()
				{
					@Override
					public void run()
					{
						try
						{
							logger.debug("Executing job " + j.getClass());
							j.execute();
						}
						catch (Exception e)
						{
							logger.error("",e);
						}
					}
				},
				delay,
				period));
		}
	}
	
	@Override
	public void destroy()
	{
		if (jobTimer != null)
			jobTimer.cancel();
	}
	
	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}
	
	public void setDelay(long delay)
	{
		this.delay = delay;
	}
	
	public void setPeriod(long period)
	{
		this.period = period;
	}
	
	public void setJobs(List<Job> jobs)
	{
		this.jobs = jobs;
	}

}
