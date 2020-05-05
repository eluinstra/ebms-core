package nl.clockwork.ebms.cpa.dao;

import java.util.List;
import java.util.Optional;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;

import nl.clockwork.ebms.dao.DAOException;

public interface CPADAO
{
	boolean existsCPA(String cpaId) throws DAOException;
	Optional<CollaborationProtocolAgreement> getCPA(String cpaId) throws DAOException;
	List<String> getCPAIds() throws DAOException;
	void insertCPA(CollaborationProtocolAgreement cpa) throws DAOException;
	int updateCPA(CollaborationProtocolAgreement cpa) throws DAOException;
	int deleteCPA(String cpaId) throws DAOException;
}