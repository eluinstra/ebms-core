/*
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

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.quartz.SchedulerContext;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory
{
	ApplicationContext applicationContext;
	SchedulerContext schedulerContext;

	@Override
	protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception
	{
		val job = applicationContext.getBean(bundle.getJobDetail().getJobClass());
		val beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(job);
		val propertyValues = createPropertyValues(bundle);
		beanWrapper.setPropertyValues(propertyValues, true);
		return job;
	}

	private MutablePropertyValues createPropertyValues(TriggerFiredBundle bundle)
	{
		val result = new MutablePropertyValues();
		result.addPropertyValues(bundle.getJobDetail().getJobDataMap());
		result.addPropertyValues(bundle.getTrigger().getJobDataMap());
		if (schedulerContext != null)
			result.addPropertyValues(schedulerContext);
		return result;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
	{
		this.applicationContext = applicationContext;
	}

	@Override
	public void setSchedulerContext(SchedulerContext schedulerContext)
	{
		this.schedulerContext = schedulerContext;
		super.setSchedulerContext(schedulerContext);
	}
}
