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
package nl.clockwork.ebms.scheduler;

import org.quartz.Job;
import org.quartz.SchedulerContext;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory
{
	ApplicationContext applicationContext;
	SchedulerContext schedulerContext;

	@Override
	protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception
	{
		Job job = applicationContext.getBean(bundle.getJobDetail().getJobClass());
		BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(job);
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.addPropertyValues(bundle.getJobDetail().getJobDataMap());
		propertyValues.addPropertyValues(bundle.getTrigger().getJobDataMap());
		if (schedulerContext != null)
			propertyValues.addPropertyValues(schedulerContext);
		beanWrapper.setPropertyValues(propertyValues,true);
		return job;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
	{
		this.applicationContext = applicationContext;
	}
	
	public void setSchedulerContext(SchedulerContext schedulerContext)
	{
		this.schedulerContext = schedulerContext;
		super.setSchedulerContext(schedulerContext);
	}
}
