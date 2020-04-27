package nl.clockwork.ebms.common;

import java.security.cert.X509Certificate;
import java.util.List;

import net.sf.ehcache.Ehcache;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.CertificateMapping;

public class CertificateManager
{
	private Ehcache daoMethodCache;
	private EbMSDAO ebMSDAO;

	public List<CertificateMapping> getCertificates()
	{
		return ebMSDAO.getCertificateMappings();
	}

	public X509Certificate getCertificate(X509Certificate certificate)
	{
		return certificate != null ? ebMSDAO.getCertificateMapping(getId(certificate)).orElse(certificate) : null;
	}

	public void setCertificateMapping(CertificateMapping mapping)
	{
		String id = getId(mapping.getSource());
		if (mapping.getDestination() == null)
			ebMSDAO.deleteCertificateMapping(id);
		else
		{
			if (ebMSDAO.existsCertificateMapping(id))
				ebMSDAO.updateCertificateMapping(id,mapping);
			else
				ebMSDAO.insertCertificateMapping(id,mapping);
		}
		flushDAOMethodCache(id);
	}

	public void deleteCertificateMapping(X509Certificate source)
	{
		String key = getId(source);
		ebMSDAO.deleteCertificateMapping(key);
		flushDAOMethodCache(key);
	}
	
	private String getId(X509Certificate certificate)
	{
		return certificate.getIssuerX500Principal().getName() + certificate.getSerialNumber().toString();
	}

	private void flushDAOMethodCache(String key)
	{
		daoMethodCache.remove(MethodCacheInterceptor.getCacheKey("EbMSDAOImpl","existsCertificateMapping",key));
		daoMethodCache.remove(MethodCacheInterceptor.getCacheKey("EbMSDAOImpl","getCertificateMapping",key));
		daoMethodCache.remove(MethodCacheInterceptor.getCacheKey("EbMSDAOImpl","getCertificateMappings"));
	}

	public void setDaoMethodCache(Ehcache daoMethodCache)
	{
		this.daoMethodCache = daoMethodCache;
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}

}
