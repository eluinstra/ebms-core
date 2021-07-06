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
package nl.clockwork.ebms.service.cpa.url;

import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.cpa.URLMapper;

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
			if (log.isDebugEnabled())
				log.debug("SetURLMapping " + urlMapping);
			urlMapper.setURLMapping(urlMapping);
		}
		catch (Exception e)
		{
			log.error("SetURLMapping " + urlMapping,e);
			throw new URLMappingServiceException(e);
		}
	}

	@Override
	public void deleteURLMapping(String source) throws URLMappingServiceException
	{
		try
		{
			log.debug("DeleteURLMapping " + source);
			urlMapper.deleteURLMapping(source);
		}
		catch (Exception e)
		{
			log.error("DeleteURLMapping " + source,e);
			throw new URLMappingServiceException(e);
		}
	}

	@Override
	public List<URLMapping> getURLMappings() throws URLMappingServiceException
	{
		try
		{
			log.debug("GetURLMappings");
			return urlMapper.getURLs();
		}
		catch (Exception e)
		{
			log.error("GetURLMappings",e);
			throw new URLMappingServiceException(e);
		}
	}
}
