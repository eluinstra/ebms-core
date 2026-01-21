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
package nl.clockwork.ebms.api.cpa.url;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.common.cpa.url.URLMapping;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
class URLMappingControllerImpl implements URLMappingController
{
	@NonNull
	URLMappingRepository urlMappingRepository;

	@Override
	public void setURLMapping(URLMapping urlMapping) throws URLMappingControllerException
	{
		try
		{
			setURLMappingImpl(urlMapping);
		}
		catch (URLMappingControllerException e)
		{
			log.error("SetURLMapping " + urlMapping, e);
			throw e;
		}
		catch (Exception e)
		{
			log.error("SetURLMapping " + urlMapping, e);
			throw new URLMappingControllerException(e);
		}
	}

	protected void setURLMappingImpl(URLMapping urlMapping)
	{
		if (log.isDebugEnabled())
			log.debug("SetURLMapping " + urlMapping);
		validate(urlMapping.getSource(), "Source invalid");
		if (StringUtils.isEmpty(urlMapping.getDestination()))
			deleteURLMapping(urlMapping.getSource());
		else
		{
			validate(urlMapping.getDestination(), "Destination invalid");
			save(urlMapping);
		}
	}

	private void validate(String url, String errorString)
	{
		try
		{
			new URL(url);
		}
		catch (MalformedURLException e)
		{
			throw new IllegalArgumentException(errorString, e);
		}
	}

	private void save(URLMapping urlMapping)
	{
		if (urlMappingRepository.existsURLMapping(urlMapping.getSource()))
			urlMappingRepository.updateURLMapping(urlMapping);
		else
			urlMappingRepository.insertURLMapping(urlMapping);
	}

	@Override
	public void deleteURLMapping(String source) throws URLMappingControllerException
	{
		try
		{
			deleteURLMappingImpl(source);
		}
		catch (URLMappingControllerException e)
		{
			log.error("DeleteURLMapping " + source, e);
			throw e;
		}
		catch (Exception e)
		{
			log.error("DeleteURLMapping " + source, e);
			throw new URLMappingControllerException(e);
		}
	}

	protected void deleteURLMappingImpl(String source)
	{
		log.debug("DeleteURLMapping " + source);
		if (urlMappingRepository.deleteURLMapping(source) == 0)
			throw new URLNotFoundException();
	}

	@Override
	public List<URLMapping> getURLMappings() throws URLMappingControllerException
	{
		try
		{
			return getURLMappingsImpl();
		}
		catch (URLMappingControllerException e)
		{
			log.error("GetURLMappings", e);
			throw e;
		}
		catch (Exception e)
		{
			log.error("GetURLMappings", e);
			throw new URLMappingControllerException(e);
		}
	}

	protected List<URLMapping> getURLMappingsImpl()
	{
		log.debug("GetURLMappings");
		return urlMappingRepository.getURLMappings();
	}

	@Override
	public void deleteCache()
	{
		try
		{
			deleteCacheImpl();
		}
		catch (Exception e)
		{
			log.error("DeleteCache", e);
			throw new URLMappingControllerException(e);
		}
	}

	protected void deleteCacheImpl()
	{
		log.debug("DeleteCache");
		urlMappingRepository.clearCache();
	}
}
