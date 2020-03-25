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
package nl.clockwork.ebms;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;

public class FileManager implements InitializingBean, Runnable
{
	private static FileManager fileManager;
	private String attachmentDirectory;
	private Path tempDir;
	private ConcurrentLinkedDeque<File> files = new ConcurrentLinkedDeque<>();
	private ConcurrentLinkedDeque<File> markedFilesForDeletion = new ConcurrentLinkedDeque<>();

	@Override
	public void afterPropertiesSet() throws Exception
	{
		try
		{
			fileManager = this;
			Path attachmentDirectory = StringUtils.isNotEmpty(this.attachmentDirectory) ? Paths.get(this.attachmentDirectory) : null;
			this.tempDir = attachmentDirectory == null ? Files.createTempDirectory("ebms-tmp-") : Files.createTempDirectory(attachmentDirectory,"ebms-tmp-");
			Thread thread = new Thread(fileManager);
			thread.setDaemon(true);
			thread.start();
		}
		catch (IOException e)
		{
			//do nothing
		}
	}
	
	public static File createTempFile() throws IOException
	{
		File file = Files.createTempFile(fileManager.tempDir,"ebms-",".tmp").toFile();
		fileManager.files.add(file);
		return file;
	}

	public static void markFileForDeletion(File file)
	{
		fileManager.markedFilesForDeletion.add(file);
	}

	@Override
	public void run()
	{
  	while (true)
  	{
  		long start = new Date().getTime();
			deleteMarkedFiles();
			deleteOldFiles();
			long end = new Date().getTime();
			long sleep = 10000 - (end - start);
			sleep(sleep);
  	}
	}

	private void deleteMarkedFiles()
	{
		while (markedFilesForDeletion.size() > 0)
		{
			File file = markedFilesForDeletion.peek();
			if (delete(file))
				files.remove(file);
			markedFilesForDeletion.remove();
		}
	}

	private void deleteOldFiles()
	{
		long treshold = new Date().getTime() - 300000;
		while (files.size() > 0)
		{
			File file = files.peek();
			if (file.lastModified() < treshold)
			{
				if (delete(file))
					files.remove();
				else
					break;
			}
			else
				break;
		}
	}

	private boolean delete(File file)
	{
		try
		{
			if (file.exists())
				return file.delete();
			else
				return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	private void sleep(long millis)
	{
		try
		{
			if (millis > 0)
				Thread.sleep(millis);
		}
		catch (InterruptedException e)
		{
			//do nothing
		}
	}

	public void setAttachmentDirectory(String attachmentDirectory) throws IOException
	{
		this.attachmentDirectory = attachmentDirectory;
	}
}
