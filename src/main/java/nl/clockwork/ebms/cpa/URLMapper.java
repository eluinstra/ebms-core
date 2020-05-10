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
package nl.clockwork.ebms.cpa;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import net.sf.ehcache.Ehcache;
import nl.clockwork.ebms.common.InvalidURLException;
import nl.clockwork.ebms.common.MethodCacheInterceptor;
import nl.clockwork.ebms.service.model.URLMapping;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class URLMapper
{
	@NonNull
	Ehcache daoMethodCache;
	@NonNull
	URLMappingDAO urlMappingDAO;

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

	public void setURLMapping(URLMapping urlMapping) throws InvalidURLException
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
		flushDAOMethodCache(urlMapping.getSource());
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
		urlMappingDAO.deleteURLMapping(source);
		flushDAOMethodCache(source);
	}

	private void flushDAOMethodCache(String source)
	{
		//val targetName = urlMappingDAO.toString().replaceFirst("^(.*\\.)*([^@]*)@.*$","$2");
		daoMethodCache.remove(MethodCacheInterceptor.getCacheKey(urlMappingDAO.getTargetName(),"existsURLMapping",source));
		daoMethodCache.remove(MethodCacheInterceptor.getCacheKey(urlMappingDAO.getTargetName(),"getURLMapping",source));
		daoMethodCache.remove(MethodCacheInterceptor.getCacheKey(urlMappingDAO.getTargetName(),"getURLMappings"));
	}
}