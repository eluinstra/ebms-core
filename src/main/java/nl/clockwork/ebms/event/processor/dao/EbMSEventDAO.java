package nl.clockwork.ebms.event.processor.dao;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import nl.clockwork.ebms.EbMSAction;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.event.processor.EbMSEvent;
import nl.clockwork.ebms.event.processor.EbMSEventStatus;

public interface EbMSEventDAO
{
	void executeTransaction(DAOTransactionCallback callback) throws DAOException;

	Optional<EbMSAction> getMessageAction(String messageId) throws DAOException;
	List<EbMSEvent> getEventsBefore(Date timestamp) throws DAOException;
	List<EbMSEvent> getEventsBefore(Date timestamp, int maxNr) throws DAOException;
	void insertEvent(EbMSEvent event) throws DAOException;
	void insertEventLog(String messageId, Date timestamp, String uri, EbMSEventStatus status, String errorMessage) throws DAOException;
	void updateEvent(EbMSEvent event) throws DAOException;
	void deleteEvent(String messageId) throws DAOException;
}