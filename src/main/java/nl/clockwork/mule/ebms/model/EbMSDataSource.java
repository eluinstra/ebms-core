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
package nl.clockwork.mule.ebms.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

public class EbMSDataSource implements DataSource
{
	private DataSource dataSource;
	private String contentId;
	private String name;
	
	public EbMSDataSource(DataSource dataSource, String componentId)
	{
		this(dataSource,componentId,null);
	}
	
	public EbMSDataSource(DataSource dataSource, String contentId, String name)
	{
		this.dataSource = dataSource;
		this.contentId = contentId;
		this.name = name;
	}
	
	@Override
	public String getContentType()
	{
		return dataSource.getContentType();
	}

	@Override
	public InputStream getInputStream() throws IOException
	{
		return dataSource.getInputStream();
	}

	@Override
	public String getName()
	{
		return name == null ? dataSource.getName() : name;
	}

	@Override
	public OutputStream getOutputStream() throws IOException
	{
		return dataSource.getOutputStream();
	}

	public DataSource getDataSource()
	{
		return dataSource;
	}
	
	public String getContentId()
	{
		return contentId;
	}

}
