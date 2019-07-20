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
package nl.clockwork.ebms.common;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.URLMapping;

import org.ehcache.core.Ehcache;
import org.springframework.util.StringUtils;

public class URLManager
{
	private Ehcache<String,Object> methodCache;
	private EbMSDAO ebMSDAO;

	public List<URLMapping> getURLs()
	{
		return ebMSDAO.getURLMappings();
	}

	public String getURL(String source)
	{
		if (!StringUtils.isEmpty(source))
			return ebMSDAO.getURLMapping(source).orElse(source);
		else
			return source;
	}

	public void setURLMapping(URLMapping urlMapping) throws InvalidURLException
	{
		if (StringUtils.isEmpty(urlMapping.getDestination()))
			ebMSDAO.deleteURLMapping(urlMapping.getSource());
		else
		{
			validate(urlMapping);
			if (ebMSDAO.existsURLMapping(urlMapping.getSource()))
				ebMSDAO.updateURLMapping(urlMapping);
			else
				ebMSDAO.insertURLMapping(urlMapping);
		}
		flushURLMethodCache(urlMapping.getSource());
	}

	private void validate(URLMapping urlMapping) throws InvalidURLException
	{
		try
		{
			new URL(urlMapping.getSource());
		}
		catch (MalformedURLException e)
		{
			throw new InvalidURLException("Source invalid",e);
		}
		try
		{
			new URL(urlMapping.getDestination());
		}
		catch (MalformedURLException e)
		{
			throw new InvalidURLException("Destination invalid",e);
		}
	}

	public void deleteURLMapping(String source)
	{
		ebMSDAO.deleteURLMapping(source);
		flushURLMethodCache(source);
	}

	private void flushURLMethodCache(String source)
	{
		methodCache.remove(MethodCacheInterceptor.getCacheKey("EbMSDAOImpl","existsURLMapping",source));
		methodCache.remove(MethodCacheInterceptor.getCacheKey("EbMSDAOImpl","getURLMapping",source));
		methodCache.remove(MethodCacheInterceptor.getCacheKey("EbMSDAOImpl","getURLMappings"));
	}

	public void setMethodCache(Ehcache<String,Object> methodCache)
	{
		this.methodCache = methodCache;
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}

}
