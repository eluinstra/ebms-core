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

import java.util.List;

import net.sf.ehcache.Ehcache;
import nl.clockwork.ebms.dao.EbMSDAO;

import org.springframework.util.StringUtils;

public class URLManager
{
	private Ehcache methodCache;
	private EbMSDAO ebMSDAO;

	public List<String> getUrls()
	{
		return ebMSDAO.getUrls();
	}

	public String getUrl(String source)
	{
		String result = ebMSDAO.getUrl(source);
		return result == null ? source : result;
	}

	public void setUrl(String source, String destination)
	{
		if (StringUtils.isEmpty(destination))
			ebMSDAO.deleteUrl(source);
		else
			if (ebMSDAO.existsUrl(source))
				ebMSDAO.updateUrl(source,destination);
			else
				ebMSDAO.insertUrl(source,destination);
		flushUrlMethodCache(source);
	}

	private void flushUrlMethodCache(String cpaId)
	{
		methodCache.remove(MethodCacheInterceptor.getCacheKey("EbMSDAOImpl","getUrl",cpaId));
	}

	public void setMethodCache(Ehcache methodCache)
	{
		this.methodCache = methodCache;
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}

}
