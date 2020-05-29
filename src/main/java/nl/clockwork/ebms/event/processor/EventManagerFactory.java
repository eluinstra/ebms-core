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
package nl.clockwork.ebms.event.processor;

import org.springframework.beans.factory.FactoryBean;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.cpa.CPAManager;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class EventManagerFactory implements FactoryBean<EventManager>
{
	@NonNull
	EbMSEventDAO ebMSEventDAO;
	@NonNull
	CPAManager cpaManager;
	String serverId;
	boolean autoRetryResponse;
	int nrAutoRetries;
	int autoRetryInterval;
	
	@Override
	public EventManager getObject() throws Exception
	{
		return autoRetryResponse ? new EventManagerRetryAck(ebMSEventDAO,cpaManager,serverId,nrAutoRetries,autoRetryInterval) : new EventManager(ebMSEventDAO,cpaManager,serverId);
	}
	
	@Override
	public Class<?> getObjectType()
	{
		return EventManager.class;
	}
	
	@Override
	public boolean isSingleton()
	{
		return true;
	}
	
}
