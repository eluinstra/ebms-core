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


import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class URLMappingServiceImpl implements URLMappingService
{
	@NonNull
	URLMapper urlMapper;

	@Override
	public void setURLMapping(URLMapping urlMapping) throws URLMappingServiceException
	{
		try
		{
			setURLMappingImpl(urlMapping);
		}
		catch (URLMappingServiceException e)
		{
			log.error("SetURLMapping " + urlMapping,e);
			throw e;
		}
		catch (Exception e)
		{
			log.error("SetURLMapping " + urlMapping,e);
			throw new URLMappingServiceException(e);
		}
	}

	protected void setURLMappingImpl(URLMapping urlMapping)
	{
		if (log.isDebugEnabled())
			log.debug("SetURLMapping " + urlMapping);
		urlMapper.setURLMapping(urlMapping);
	}

	@Override
	public void deleteURLMapping(String source) throws URLMappingServiceException
	{
		try
		{
			deleteURLMappingImpl(source);
		}
		catch (URLMappingServiceException e)
		{
			log.error("DeleteURLMapping " + source,e);
			throw e;
		}
		catch (Exception e)
		{
			log.error("DeleteURLMapping " + source,e);
			throw new URLMappingServiceException(e);
		}
	}

	protected void deleteURLMappingImpl(String source)
	{
		log.debug("DeleteURLMapping " + source);
		if (urlMapper.deleteURLMapping(source) == 0)
			throw new URLNotFoundException();
	}

	@Override
	public List<URLMapping> getURLMappings() throws URLMappingServiceException
	{
		try
		{
			return getURLMappingsImpl();
		}
		catch (URLMappingServiceException e)
		{
			log.error("GetURLMappings",e);
			throw e;
		}
		catch (Exception e)
		{
			log.error("GetURLMappings",e);
			throw new URLMappingServiceException(e);
		}
	}

	protected List<URLMapping> getURLMappingsImpl()
	{
		log.debug("GetURLMappings");
		return urlMapper.getURLs();
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
			log.error("DeleteCache",e);
			throw new URLMappingServiceException(e);
		}
	}

	protected void deleteCacheImpl()
	{
		log.debug("DeleteCache");
		urlMapper.deleteCache();
	}
}
