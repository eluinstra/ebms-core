package nl.clockwork.ebms.cpa;

import static nl.clockwork.ebms.cpa.URLMappingMapper.URLMappingDSL.destination;
import static nl.clockwork.ebms.cpa.URLMappingMapper.URLMappingDSL.source;
import static nl.clockwork.ebms.cpa.URLMappingMapper.URLMappingDSL.urlMapping;
import static org.mybatis.dynamic.sql.SqlBuilder.count;
import static org.mybatis.dynamic.sql.SqlBuilder.deleteFrom;
import static org.mybatis.dynamic.sql.SqlBuilder.insert;
import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;
import static org.mybatis.dynamic.sql.SqlBuilder.select;
import static org.mybatis.dynamic.sql.SqlBuilder.update;

import java.util.List;
import java.util.Optional;

import org.mybatis.dynamic.sql.render.RenderingStrategies;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.service.cpa.url.URLMapping;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class URLMappingDAOImpl implements URLMappingDAO
{
	URLMappingMapper mapper;
	
	@Override
	public boolean existsURLMapping(String source_) throws DAOException
	{
    val selectStatement = select(count())
        .from(urlMapping)
        .where(source,isEqualTo(source_))
        .build()
        .render(RenderingStrategies.MYBATIS3);
		return mapper.count(selectStatement) > 0;
	}

	@Override
	public Optional<String> getURLMapping(String source_) throws DAOException
	{
    val selectStatement = select(source,destination)
        .from(urlMapping)
        .where(source,isEqualTo(source_))
        .build()
        .render(RenderingStrategies.MYBATIS3);
    return mapper.selectOne(selectStatement).map(m -> m.getDestination());
	}

	@Override
	public List<URLMapping> getURLMappings() throws DAOException
	{
    val selectStatement = select(source,destination)
        .from(urlMapping)
        .build()
        .render(RenderingStrategies.MYBATIS3);
    return mapper.selectMany(selectStatement);
	}

	@Override
	public void insertURLMapping(URLMapping urlMapping_) throws DAOException
	{
		val insertStatement = insert(urlMapping_)
        .into(urlMapping)
        .map(source).toProperty("source")
        .map(destination).toProperty("destination")
        .build()
        .render(RenderingStrategies.MYBATIS3);
		mapper.insert(insertStatement);
	}

	@Override
	public int updateURLMapping(URLMapping urlMapping_) throws DAOException
	{
		val updateStatement = update(urlMapping)
        .set(destination).equalTo(urlMapping_.getDestination())
        .where(source,isEqualTo(urlMapping_.getSource()))
        .build()
        .render(RenderingStrategies.MYBATIS3);
		return mapper.update(updateStatement);
	}

	@Override
	public int deleteURLMapping(String source_) throws DAOException
	{
		val deleteStatement = deleteFrom(urlMapping)
        .where(source,isEqualTo(source_))
        .build()
        .render(RenderingStrategies.MYBATIS3);
		return mapper.delete(deleteStatement);
	}

	@Override
	public String getTargetName()
	{
		return getClass().getSimpleName();
	}
}
