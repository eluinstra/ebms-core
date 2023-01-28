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


import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public interface WithFile
{
	default String readFile(String path)
	{
		return readFileS(path);
	}

	static String readFileS(String path)
	{
		try
		{
			Path filePath = Paths.get(ClassLoader.getSystemResource(path).toURI());
			StringBuilder contentBuilder = new StringBuilder();
			try (Stream<String> stream = Files.lines(filePath,StandardCharsets.UTF_8))
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