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
package nl.clockwork.ebms.model;

import java.io.File;

import javax.activation.FileDataSource;

import nl.clockwork.ebms.FileManager;

public class EbMSFileDataSource extends FileDataSource
{
	private String name;
	private String contentType;

	public EbMSFileDataSource(String name, String contentType, File file)
	{
		super(file);
		this.name = name;
		this.contentType = contentType;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public String getContentType()
	{
		return contentType;
	}

	@Override
	protected void finalize() throws Throwable
	{
		FileManager.markFileForDeletion(getFile());
		super.finalize();
	}
}
