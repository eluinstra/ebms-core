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
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedDeque;

public class FileManager implements Runnable
{
	private static FileManager fileManager;
	private Path tempDir;
	private ConcurrentLinkedDeque<File> files = new ConcurrentLinkedDeque<>();
	private ConcurrentLinkedDeque<File> markedFilesForDeletion = new ConcurrentLinkedDeque<>();

	static
	{
		try
		{
			fileManager = new FileManager();
			Thread thread = new Thread(fileManager);
			thread.setDaemon(true);
			thread.start();
		}
		catch (IOException e)
		{
			//do nothing
		}
	}
	
	public FileManager() throws IOException
	{
		tempDir = Files.createTempDirectory("ebms-tmp-");
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
			while (markedFilesForDeletion.size() > 0)
			{
				File file = markedFilesForDeletion.peek();
				delete(file);
				markedFilesForDeletion.remove();
				files.remove(file);
			}
			long t = new Date().getTime() - 600000;
			while (files.size() > 0)
			{
				File file = files.peek();
				if (file.lastModified() < t)
					delete(file);
				else
					break;
				files.remove();
			}
			long end = new Date().getTime();
			long sleep = 10000 - (end - start);
			sleep(sleep);
  	}
	}

	private void delete(File file)
	{
		try
		{
			file.delete();
		}
		catch (Exception e)
		{
			//do nothing
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
}
