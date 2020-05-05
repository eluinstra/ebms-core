package nl.clockwork.ebms.cpa.dao;

import java.util.List;
import java.util.Optional;

import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.service.model.URLMapping;

public interface URLMappingDAO
{
	boolean existsURLMapping(String source) throws DAOException;
	Optional<String> getURLMapping(String source) throws DAOException;
	List<URLMapping> getURLMappings() throws DAOException;
	void insertURLMapping(URLMapping urlMapping) throws DAOException;
	int updateURLMapping(URLMapping urlMapping) throws DAOException;
	int deleteURLMapping(String source) throws DAOException;
}