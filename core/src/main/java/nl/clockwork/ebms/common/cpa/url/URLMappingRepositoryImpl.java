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
package nl.clockwork.ebms.common.cpa.url;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.client.delivery.task.URLMappingRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@CacheConfig(cacheNames = {"URLMapping"})
class URLMappingRepositoryImpl implements nl.clockwork.ebms.api.cpa.url.URLMappingRepository, URLMappingRepository
{
	@NonNull
	JdbcTemplate jdbcTemplate;

	@Override
	@CacheEvict(cacheNames = "URLMapping", allEntries = true)
	public void clearCache()
	{
		// do nothing
	}

	@Override
	@Cacheable(cacheNames = "URLMapping", keyGenerator = "ebMSKeyGenerator")
	public boolean existsURLMapping(String source)
	{
		Integer result = jdbcTemplate.queryForObject("select count(*) from url_mapping where source = ?", Integer.class, source);
		return result != null && result > 0;
	}

	@Override
	@Cacheable(cacheNames = "URLMapping", keyGenerator = "ebMSKeyGenerator")
	public String getURL(String source)
	{
		if (!StringUtils.isEmpty(source))
			return getURLMapping(source).orElse(source);
		else
			return source;
	}

	private Optional<String> getURLMapping(String source)
	{
		try
		{
			return Optional.of(jdbcTemplate.queryForObject("select destination from url_mapping where source = ?", String.class, source));
		}
		catch (EmptyResultDataAccessException e)
		{
			return Optional.empty();
		}
	}

	@Override
	@Cacheable(cacheNames = "URLMapping", keyGenerator = "ebMSKeyGenerator")
	public List<URLMapping> getURLMappings()
	{
		return jdbcTemplate.query("select source, destination from url_mapping order by source asc", new RowMapper<URLMapping>()
		{
			@Override
			public URLMapping mapRow(@org.jspecify.annotations.NonNull ResultSet rs, int nr) throws SQLException
			{
				return new URLMapping(rs.getString("source"), rs.getString("destination"));
			}
		});
	}

	@Override
	@CacheEvict(cacheNames = "URLMapping", allEntries = true)
	public String insertURLMapping(URLMapping urlMapping)
	{
		jdbcTemplate.update("insert into url_mapping (source,destination) values (?,?)", urlMapping.getSource(), urlMapping.getDestination());
		return urlMapping.getSource();
	}

	@Override
	@CacheEvict(cacheNames = "URLMapping", allEntries = true)
	public int updateURLMapping(URLMapping urlMapping)
	{
		return jdbcTemplate.update("update url_mapping set destination = ? where source = ?", urlMapping.getDestination(), urlMapping.getSource());
	}

	@Override
	@CacheEvict(cacheNames = "URLMapping", allEntries = true)
	public int deleteURLMapping(String source)
	{
		return jdbcTemplate.update("delete from url_mapping where source = ?", source);
	}
}
