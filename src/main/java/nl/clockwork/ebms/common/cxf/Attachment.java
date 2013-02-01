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
package nl.clockwork.ebms.common.cxf;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.DataSource;

public class Attachment implements org.apache.cxf.message.Attachment
{
	private Map<String,String> headers = new HashMap<String,String>();
	//headers.put("Content-ID","<1>");
	//headers.put("Content-Type","application/xml");
	//headers.put("Content-Transfer-Encoding","binary");
	//headers.put("Content-Disposition","attachment; filename=" + ds.getName());
	private String id;
	private DataSource dataSource;
	
	public Attachment(String id, DataSource dataSource)
	{
		this.id = id;
		this.dataSource = dataSource;
	}
	
	@Override
	public boolean isXOP()
	{
		return false;
	}
	
	@Override
	public String getId()
	{
		return id;
	}
	
	@Override
	public Iterator<String> getHeaderNames()
	{
		return headers.keySet().iterator();
	}
	
	@Override
	public String getHeader(String key)
	{
		return headers.get(key);
	}
	
	@Override
	public DataHandler getDataHandler()
	{
		return new DataHandler(dataSource);
	}
};
