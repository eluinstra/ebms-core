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
package nl.clockwork.ebms.model;

import nl.clockwork.ebms.model.ebxml.Service;

public class EbMSService extends Service
{
	public EbMSService(String value)
	{
		this(null,value);
	}

	public EbMSService(String type, String value)
	{
		this.type = type;
		this.value = value;
	}
	
	public boolean compare(Service service)
	{
		if (service == null)
			return false;
		return (this.type == service.getType() || (this.type != null && this.type.equals(service.getType())))
				&& (this.value == null || service.getValue() == null || this.value.equals(service.getValue()));
	}
}
