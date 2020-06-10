package nl.clockwork.ebms.cpa;

import static nl.clockwork.ebms.cpa.CertificateMappingMapper.CertificateMappingDSL.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.session.SqlSessionFactory;
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
	SqlSessionFactory sqlSessionFactory;
	
	@Override
	public boolean existsCertificateMapping(String id_, String cpaId_) throws DAOException
	{
		try (val session = sqlSessionFactory.openSession())
		{
			val mapper = session.getMapper(CertificateMappingMapper.class);
      val selectStatement = select(count())
          .from(certificateMapping)
          .where(id,isEqualTo(id_))
          .and(cpaId,isEqualTo(cpaId_))
          .build()
          .render(RenderingStrategies.MYBATIS3);
			return mapper.count(selectStatement) > 0;
		}
	}

	@Override
	public Optional<X509Certificate> getCertificateMapping(String id_, String cpaId_) throws DAOException
	{
		try (val session = sqlSessionFactory.openSession())
		{
			val mapper = session.getMapper(CertificateMappingMapper.class);
      val selectStatement = select(source,destination)
          .from(certificateMapping)
          .where(id,isEqualTo(id_))
          .and(cpaId,isEqualTo(cpaId_))
          .build()
          .render(RenderingStrategies.MYBATIS3);
      return mapper.selectOne(selectStatement).map(m -> m.getDestination());
		}
	}

	@Override
	public List<CertificateMapping> getCertificateMappings() throws DAOException
	{
		try (val session = sqlSessionFactory.openSession())
		{
			val mapper = session.getMapper(CertificateMappingMapper.class);
      val selectStatement = select(source,destination)
          .from(certificateMapping)
          .build()
          .render(RenderingStrategies.MYBATIS3);
      return mapper.selectMany(selectStatement);
		}
	}

	@Override
	public void insertCertificateMapping(CertificateMapping mapping_) throws DAOException
	{
		try (val session = sqlSessionFactory.openSession())
		{
			val mapper = session.getMapper(CertificateMappingMapper.class);
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
	}

	@Override
	public int updateCertificateMapping(CertificateMapping mapping_) throws DAOException
	{
		try (val session = sqlSessionFactory.openSession())
		{
			val mapper = session.getMapper(CertificateMappingMapper.class);
			val updateStatement = update(certificateMapping)
          .set(destination).equalTo(mapping_.getDestination())
          .set(cpaId).equalTo(mapping_.getCpaId())
          .where(id,isEqualTo(mapping_.getId()))
          .build()
          .render(RenderingStrategies.MYBATIS3);
			return mapper.update(updateStatement);
		}
	}

	@Override
	public int deleteCertificateMapping(String id_, String cpaId_) throws DAOException
	{
		try (val session = sqlSessionFactory.openSession())
		{
			val mapper = session.getMapper(CertificateMappingMapper.class);
			val deleteStatement = deleteFrom(certificateMapping)
          .where(id,isEqualTo(id_))
          .build()
          .render(RenderingStrategies.MYBATIS3);
			return mapper.delete(deleteStatement);
		}
	}

	@Override
	public String getTargetName()
	{
		return getClass().getSimpleName();
	}
}
