package nl.clockwork.ebms.cpa.certificate;

import static nl.clockwork.ebms.cpa.certificate.CertificateMappingMapper.CertificateMappingDSL.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

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
    val s = select(count())
        .from(certificateMapping)
        .where(id,isEqualTo(id_))
        .and(cpaId,isNull().when(() -> cpaId_ == null))
        .and(cpaId,isEqualTo(cpaId_).when(id -> id != null))
        .build()
        .render(RenderingStrategies.MYBATIS3);
		return mapper.count(s) > 0;
	}

	@Override
	public Optional<X509Certificate> getCertificateMapping(String id_, String cpaId_) throws DAOException
	{
    val s = select(source,destination,cpaId)
        .from(certificateMapping)
        .where(id,isEqualTo(id_))
        .and(cpaId,isNull().when(() -> cpaId_ == null))
        .and(cpaId,isEqualTo(cpaId_).when(id -> id != null))
        .build()
        .render(RenderingStrategies.MYBATIS3);
    return mapper.selectOne(s).map(m -> m.getDestination());
	}

	@Override
	public List<CertificateMapping> getCertificateMappings() throws DAOException
	{
    val s = select(source,destination,cpaId)
        .from(certificateMapping)
        .build()
        .render(RenderingStrategies.MYBATIS3);
    List<CertificateMapping> selectMany = mapper.selectMany(s);
		return selectMany;
	}

	@Override
	public void insertCertificateMapping(CertificateMapping mapping_) throws DAOException
	{
		val s = insert(mapping_)
        .into(certificateMapping)
        .map(id).toProperty("id")
        .map(source).toProperty("source")
        .map(destination).toProperty("destination")
        .map(cpaId).toProperty("cpaId")
        .build()
        .render(RenderingStrategies.MYBATIS3);
		mapper.insert(s);
	}

	@Override
	public int updateCertificateMapping(CertificateMapping mapping_) throws DAOException
	{
		val s = update(certificateMapping)
        .set(destination).equalTo(mapping_.getDestination())
        .set(cpaId).equalTo(mapping_.getCpaId())
        .where(id,isEqualTo(mapping_.getId()))
        .and(cpaId,isNull().when(() -> mapping_.getCpaId() == null))
        .and(cpaId,isEqualTo(mapping_.getCpaId()).when(id -> id != null))
        .build()
        .render(RenderingStrategies.MYBATIS3);
		return mapper.update(s);
	}

	@Override
	public int deleteCertificateMapping(String id_, String cpaId_) throws DAOException
	{
		val s = deleteFrom(certificateMapping)
        .where(id,isEqualTo(id_))
        .and(cpaId,isNull().when(() -> cpaId_ == null))
        .and(cpaId,isEqualTo(cpaId_).when(id -> id != null))
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
