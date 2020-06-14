package nl.clockwork.ebms.cpa.certificate;

import static nl.clockwork.ebms.cpa.certificate.CertificateMappingMapper.CertificateMappingDSL.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

import org.mybatis.dynamic.sql.render.RenderingStrategies;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.service.cpa.certificate.CertificateMapping;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class CertificateMappingDAOImpl implements CertificateMappingDAO
{
	@NonNull
	CertificateMappingMapper mapper;
	
	@Override
	public boolean existsCertificateMapping(String id, String cpaId) throws DAOException
	{
    val s = select(count())
        .from(certificateMappingTable)
        .where(certificateMappingTable.id,isEqualTo(id))
        .and(certificateMappingTable.cpaId,isNull().when(() -> cpaId == null))
        .and(certificateMappingTable.cpaId,isEqualTo(cpaId).when(v -> v != null))
        .build()
        .render(RenderingStrategies.MYBATIS3);
		return mapper.count(s) > 0;
	}

	@Override
	public Optional<X509Certificate> getCertificateMapping(String id, String cpaId) throws DAOException
	{
    val s = select(certificateMappingTable.all)
        .from(certificateMappingTable)
        .where(certificateMappingTable.id,isEqualTo(id))
        .and(certificateMappingTable.cpaId,isNull().when(() -> cpaId == null))
        .and(certificateMappingTable.cpaId,isEqualTo(cpaId).when(v -> v != null))
        .build()
        .render(RenderingStrategies.MYBATIS3);
    return mapper.selectOne(s).map(m -> m.getDestination());
	}

	@Override
	public List<CertificateMapping> getCertificateMappings() throws DAOException
	{
    val s = select(certificateMappingTable.all)
        .from(certificateMappingTable)
        .build()
        .render(RenderingStrategies.MYBATIS3);
    List<CertificateMapping> selectMany = mapper.selectMany(s);
		return selectMany;
	}

	@Override
	public void insertCertificateMapping(CertificateMapping mapping) throws DAOException
	{
		val s = insert(mapping)
        .into(certificateMappingTable)
        .map(id).toProperty("id")
        .map(source).toProperty("source")
        .map(destination).toProperty("destination")
        .map(cpaId).toProperty("cpaId")
        .build()
        .render(RenderingStrategies.MYBATIS3);
		mapper.insert(s);
	}

	@Override
	public int updateCertificateMapping(CertificateMapping mapping) throws DAOException
	{
		val s = update(certificateMappingTable)
        .set(destination).equalTo(mapping.getDestination())
        .set(cpaId).equalTo(mapping.getCpaId())
        .where(id,isEqualTo(mapping.getId()))
        .and(cpaId,isNull().when(() -> mapping.getCpaId() == null))
        .and(cpaId,isEqualTo(mapping.getCpaId()).when(v -> v != null))
        .build()
        .render(RenderingStrategies.MYBATIS3);
		return mapper.update(s);
	}

	@Override
	public int deleteCertificateMapping(String id, String cpaId) throws DAOException
	{
		val s = deleteFrom(certificateMappingTable)
        .where(certificateMappingTable.id,isEqualTo(id))
        .and(certificateMappingTable.cpaId,isNull().when(() -> cpaId == null))
        .and(certificateMappingTable.cpaId,isEqualTo(cpaId).when(v -> v != null))
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
