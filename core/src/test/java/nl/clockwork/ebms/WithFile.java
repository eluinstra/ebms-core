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
package nl.clockwork.ebms;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.val;

public interface WithFile
{
	default String readFile(String path)
	{
		return readFileS(path);
	}

	default File getFile(String path)
	{
		try
		{
			val filePath = Paths.get(ClassLoader.getSystemResource(path).toURI());
			return filePath.toFile();
		}
		catch (URISyntaxException e)
		{
			throw new IllegalStateException(e);
		}
	}

	static String readFileS(String path)
	{
		try
		{
			val filePath = Paths.get(ClassLoader.getSystemResource(path).toURI());
			val contentBuilder = new StringBuilder();
			try (val stream = Files.lines(filePath, StandardCharsets.UTF_8))
			{
				stream.forEach(s -> contentBuilder.append(s).append("\n"));
			}
			return contentBuilder.toString();
		}
		catch (URISyntaxException | IOException e)
		{
			throw new IllegalStateException(e);
		}
	}
}