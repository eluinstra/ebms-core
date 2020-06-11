package nl.clockwork.ebms.cpa;

import static nl.clockwork.ebms.cpa.CertificateMappingMapper.CertificateMappingDSL.certificateMapping;
import static nl.clockwork.ebms.cpa.CertificateMappingMapper.CertificateMappingDSL.cpaId;
import static nl.clockwork.ebms.cpa.CertificateMappingMapper.CertificateMappingDSL.destination;
import static nl.clockwork.ebms.cpa.CertificateMappingMapper.CertificateMappingDSL.id;
import static nl.clockwork.ebms.cpa.CertificateMappingMapper.CertificateMappingDSL.source;
import static org.mybatis.dynamic.sql.SqlBuilder.count;
import static org.mybatis.dynamic.sql.SqlBuilder.deleteFrom;
import static org.mybatis.dynamic.sql.SqlBuilder.insert;
import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;
import static org.mybatis.dynamic.sql.SqlBuilder.select;
import static org.mybatis.dynamic.sql.SqlBuilder.update;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

import org.mybatis.dynamic.sql.render.RenderingStrategies;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.service.cpa.certificate.CertificateMapping;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class CertificateMappingDAOImpl implements CertificateMappingDAO
{
	CertificateMappingMapper mapper;
	
	@Override
	public boolean existsCertificateMapping(String id_, String cpaId_) throws DAOException
	{
    val selectStatement = select(count())
        .from(certificateMapping)
        .where(id,isEqualTo(id_))
        .and(cpaId,isEqualTo(cpaId_))
        .build()
        .render(RenderingStrategies.MYBATIS3);
		return mapper.count(selectStatement) > 0;
	}

	@Override
	public Optional<X509Certificate> getCertificateMapping(String id_, String cpaId_) throws DAOException
	{
    val selectStatement = select(source,destination)
        .from(certificateMapping)
        .where(id,isEqualTo(id_))
        .and(cpaId,isEqualTo(cpaId_))
        .build()
        .render(RenderingStrategies.MYBATIS3);
    return mapper.selectOne(selectStatement).map(m -> m.getDestination());
	}

	@Override
	public List<CertificateMapping> getCertificateMappings() throws DAOException
	{
    val selectStatement = select(source,destination)
        .from(certificateMapping)
        .build()
        .render(RenderingStrategies.MYBATIS3);
    return mapper.selectMany(selectStatement);
	}

	@Override
	public void insertCertificateMapping(CertificateMapping mapping_) throws DAOException
	{
		val insertStatement = insert(mapping_)
        .into(certificateMapping)
        .map(id).toProperty("id")
        .map(source).toProperty("source")
        .map(destination).toProperty("destination")
        .map(cpaId).toProperty("cpaId")
        .build()
        .render(RenderingStrategies.MYBATIS3);
		mapper.insert(insertStatement);
	}

	@Override
	public int updateCertificateMapping(CertificateMapping mapping_) throws DAOException
	{
		val updateStatement = update(certificateMapping)
        .set(destination).equalTo(mapping_.getDestination())
        .set(cpaId).equalTo(mapping_.getCpaId())
        .where(id,isEqualTo(mapping_.getId()))
        .build()
        .render(RenderingStrategies.MYBATIS3);
		return mapper.update(updateStatement);
	}

	@Override
	public int deleteCertificateMapping(String id_, String cpaId_) throws DAOException
	{
		val deleteStatement = deleteFrom(certificateMapping)
        .where(id,isEqualTo(id_))
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
