package nl.clockwork.ebms.cpa;

import static nl.clockwork.ebms.cpa.CPAMapper.CPADSL.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import java.util.List;
import java.util.Optional;

import org.mybatis.dynamic.sql.render.RenderingStrategies;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.dao.DAOException;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class CPADAOImpl implements CPADAO
{
	CPAMapper mapper;

	@Override
	public boolean existsCPA(String cpaId_) throws DAOException
	{
    val s = select(count())
        .from(cpaTable)
        .where(cpaId,isEqualTo(cpaId_))
        .build()
        .render(RenderingStrategies.MYBATIS3);
		return mapper.count(s) > 0;
	}

	@Override
	public Optional<CollaborationProtocolAgreement> getCPA(String cpaId_) throws DAOException
	{
		val s = select(cpa)
        .from(cpaTable)
        .where(cpaId,isEqualTo(cpaId_))
        .build()
        .render(RenderingStrategies.MYBATIS3);
		return mapper.selectOne(s).flatMap(c -> Optional.of(c.getCpa()));
	}

	@Override
	public List<String> getCPAIds() throws DAOException
	{
		val s = select(cpaId)
        .from(cpaTable)
        .build()
        .render(RenderingStrategies.MYBATIS3);
		return mapper.selectCpaIds(s);
	}

	@Override
	public void insertCPA(CollaborationProtocolAgreement cpa_) throws DAOException
	{
		val s = insert(new CPA(cpa_))
				.into(cpaTable)
				.map(cpaId).toProperty("cpaId")
				.map(cpa).toProperty("cpa")
        .build()
        .render(RenderingStrategies.MYBATIS3);
		mapper.insert(s);
	}

	@Override
	public int updateCPA(CollaborationProtocolAgreement cpa_) throws DAOException
	{
		val s = update(cpaTable)
				.set(cpa).equalTo(cpa_)
				.where(cpaId,isEqualTo(cpa_.getCpaid()))
        .build()
        .render(RenderingStrategies.MYBATIS3);
		return mapper.update(s);
	}

	@Override
	public int deleteCPA(String cpaId_) throws DAOException
	{
		val s = deleteFrom(cpaTable)
				.where(cpaId,isEqualTo(cpaId_))
        .build()
        .render(RenderingStrategies.MYBATIS3);
		return mapper.delete(s);
	}

	@Override
	public String getTargetName()
	{
		return getClass().getSimpleName();
	}
}
