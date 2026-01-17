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

import io.vavr.control.Either;
import io.vavr.control.Option;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@CacheConfig(cacheNames = {"URLMapping"})
public class URLMappingRepository
{
	@Autowired
	@NonFinal
	@Setter
	URLMappingRepository self;
	@NonNull
	JdbcTemplate jdbcTemplate;

	@CacheEvict(cacheNames = "URLMapping", allEntries = true)
	public void clearCache()
	{
		// do nothing
	}

	@Cacheable(cacheNames = "URLMapping", keyGenerator = "ebMSKeyGenerator")
	public boolean existsURLMapping(String source)
	{
		return jdbcTemplate.queryForObject("select count(*) from url_mapping where source = ?", Integer.class, source) > 0;
	}

	public String getURL(String source)
	{
		if (!StringUtils.isEmpty(source))
			return self.getURLMapping(source).orElse(source);
		else
			return source;
	}

	@Cacheable(cacheNames = "URLMapping", keyGenerator = "ebMSKeyGenerator")
	public Optional<String> getURLMapping(String source)
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

	@Cacheable(cacheNames = "URLMapping", keyGenerator = "ebMSKeyGenerator")
	public List<URLMapping> getURLMappings()
	{
		return jdbcTemplate.query("select source, destination from url_mapping order by source asc", new RowMapper<URLMapping>()
		{
			@Override
			public URLMapping mapRow(ResultSet rs, int nr) throws SQLException
			{
				return new URLMapping(rs.getString("source"), rs.getString("destination"));
			}
		});
	}

	public void setURLMapping(URLMapping urlMapping)
	{
		if (StringUtils.isEmpty(urlMapping.getDestination()))
			self.deleteURLMapping(urlMapping.getSource());
		else
			validate(urlMapping).peek(this::save).getOrElseThrow(e -> e);
	}

	private Either<IllegalArgumentException, URLMapping> validate(URLMapping urlMapping)
	{
		return isValid(urlMapping.getSource()).map(e -> new IllegalArgumentException("Source invalid", e))
				.orElse(() -> isValid(urlMapping.getDestination()).map(e -> new IllegalArgumentException("Destination invalid", e)))
				.toEither(urlMapping)
				.swap();
	}

	private Option<MalformedURLException> isValid(String url)
	{
		try
		{
			new URL(url);
			return Option.none();
		}
		catch (MalformedURLException e)
		{
			return Option.some(e);
		}
	}

	private void save(URLMapping urlMapping)
	{
		if (self.existsURLMapping(urlMapping.getSource()))
			self.updateURLMapping(urlMapping);
		else
			self.insertURLMapping(urlMapping);
	}

	@CacheEvict(cacheNames = "URLMapping", allEntries = true)
	public String insertURLMapping(URLMapping urlMapping)
	{
		jdbcTemplate.update("insert into url_mapping (source,destination) values (?,?)", urlMapping.getSource(), urlMapping.getDestination());
		return urlMapping.getSource();
	}

	@CacheEvict(cacheNames = "URLMapping", allEntries = true)
	public int updateURLMapping(URLMapping urlMapping)
	{
		return jdbcTemplate.update("update url_mapping set destination = ? where source = ?", urlMapping.getDestination(), urlMapping.getSource());
	}

	@CacheEvict(cacheNames = "URLMapping", allEntries = true)
	public int deleteURLMapping(String source)
	{
		return jdbcTemplate.update("delete from url_mapping where source = ?", source);
	}
}
