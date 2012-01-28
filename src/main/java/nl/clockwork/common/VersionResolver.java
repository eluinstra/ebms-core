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
package nl.clockwork.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

public class VersionResolver
{
	private static final String UNKNOWN = "unknown";
	private static final String MANIFEST_NAME = "Implementation-Title: ";
	private static final String MANIFEST_VERSION = "Implementation-Version: ";
	private static final String POM_NAME = "artifactId";
	private static final String POM_VERSION = "version";

	public static String getVersionFromManifestFile() throws IOException
	{
		String result = UNKNOWN;
		BufferedReader in = new BufferedReader(new InputStreamReader(VersionResolver.class.getResourceAsStream("/META-INF/MANIFEST.MF")));
		String name = null;
		String version = null;
		String line;
		while ((line = in.readLine()) != null)
			if (line.startsWith(MANIFEST_NAME))
				name = line.substring(MANIFEST_NAME.length(),line.length());
			else if (line.startsWith(MANIFEST_VERSION))
				version = line.substring(MANIFEST_VERSION.length(),line.length());
		if (name != null && version != null)
			result = name + " " + version;
		return result;
	}
	
	public static String getVersionFromPOMPropertiesFile(String propertiesFile) throws IOException
	{
		String result = UNKNOWN;
		Properties properties = new Properties();
		properties.load(VersionResolver.class.getResourceAsStream(propertiesFile));
		if (properties.get(POM_NAME) != null  && properties.getProperty(POM_VERSION) != null)
			result = properties.get(POM_NAME) + "-" + properties.getProperty(POM_VERSION);
		return result;
	}

}
