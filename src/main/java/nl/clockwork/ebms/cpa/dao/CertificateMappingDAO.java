package nl.clockwork.ebms.cpa.dao;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.service.model.CertificateMapping;

public interface CertificateMappingDAO
{
	boolean existsCertificateMapping(String id) throws DAOException;
	Optional<X509Certificate> getCertificateMapping(String id) throws DAOException;
	List<CertificateMapping> getCertificateMappings() throws DAOException;
	void insertCertificateMapping(String id, CertificateMapping mapping) throws DAOException;
	int updateCertificateMapping(String id, CertificateMapping mapping) throws DAOException;
	int deleteCertificateMapping(String id) throws DAOException;
}