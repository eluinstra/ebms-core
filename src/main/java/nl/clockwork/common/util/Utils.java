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
package nl.clockwork.common.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.codec.binary.Base64;

public class Utils
{
	public static byte[] zip(String content) throws IOException
	{
		return zip("name",content);
	}

	public static byte[] zip(String name, String content) throws IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ZipOutputStream zout = new ZipOutputStream(out);
		ZipEntry entry = new ZipEntry(name);
		zout.putNextEntry(entry);
		zout.write(content.getBytes());
		zout.close();
		return out.toByteArray();
	}
	
	public static byte[] unzip(byte[] content) throws IOException
	{
		int BUFFER_SIZE = 2048;
    ByteArrayOutputStream out = new ByteArrayOutputStream();
		ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(content));
		ZipEntry entry = zin.getNextEntry();
		if (entry != null)
		{
			int count;
			byte buffer[] = new byte[BUFFER_SIZE];
			BufferedOutputStream bout = new BufferedOutputStream(out,BUFFER_SIZE);
			while ((count = zin.read(buffer,0,BUFFER_SIZE)) != -1)
				bout.write(buffer,0,count);
			bout.flush();
			bout.close();
			zin.close();
		}
		return out.toByteArray();
	}
	
	public static byte[] base64Decode(byte[] content)
	{
		Base64 base64 = new Base64();
		return base64.decode(content);
	}
	
	public static String getMimeType(String fileName) throws java.io.IOException
	{
		FileNameMap fileNameMap = URLConnection.getFileNameMap();
		return fileNameMap.getContentTypeFor(fileName);
	}

}
