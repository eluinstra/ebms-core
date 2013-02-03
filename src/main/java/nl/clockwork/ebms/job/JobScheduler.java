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
package nl.clockwork.ebms.job;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JobScheduler
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private Timer timer;
	private long delay;
	private long period;
	private List<Job> jobs = new ArrayList<Job>();

	public void init()
	{
		Timer timer = new Timer();
		for (final Job job : jobs)
			timer.schedule(new TimerTask()
			{
				@Override
				public void run()
				{
					logger.info("Executing job: " + job.getClass());
					job.run();
				}
			},
			delay*1000,
			period*1000);
	}
	
	public void destroy()
	{
		if (timer != null)
			timer.cancel();
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
