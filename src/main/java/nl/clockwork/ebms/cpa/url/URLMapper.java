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
package nl.clockwork.ebms.cpa.url;


import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import org.springframework.util.StringUtils;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class URLMapper
{
	@NonNull
	URLMappingDAO urlMappingDAO;
	Object urlMonitor = new Object();

	public List<URLMapping> getURLs()
	{
		return urlMappingDAO.getURLMappings();
	}

	public String getURL(String source)
	{
		if (!StringUtils.isEmpty(source))
			return urlMappingDAO.getURLMapping(source).orElse(source);
		else
			return source;
	}

	public void setURLMapping(URLMapping urlMapping)
	{
		synchronized (urlMonitor)
		{
			if (StringUtils.isEmpty(urlMapping.getDestination()))
				urlMappingDAO.deleteURLMapping(urlMapping.getSource());
			else
			{
				validate(urlMapping);
				if (urlMappingDAO.existsURLMapping(urlMapping.getSource()))
					urlMappingDAO.updateURLMapping(urlMapping);
				else
					urlMappingDAO.insertURLMapping(urlMapping);
			}
		}
	}

	private void validate(URLMapping urlMapping)
	{
		validateUrl(urlMapping.getSource(), "Source");
		validateUrl(urlMapping.getDestination(), "Destination");
	}

	private void validateUrl(String url, String propertyName)
	{
		try
		{
			new URL(url);
		}
		catch (MalformedURLException e)
		{
			throw new IllegalArgumentException(propertyName + " invalid", e);
		}
	}

	public int deleteURLMapping(String source)
	{
		return urlMappingDAO.deleteURLMapping(source);
	}

	public void deleteCache()
	{
		urlMappingDAO.clearCache();
	}
}
