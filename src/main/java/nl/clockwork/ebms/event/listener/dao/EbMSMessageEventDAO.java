package nl.clockwork.ebms.event.listener.dao;

import java.util.List;

import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.event.listener.EbMSMessageEventType;
import nl.clockwork.ebms.service.model.EbMSMessageContext;
import nl.clockwork.ebms.service.model.EbMSMessageEvent;

public interface EbMSMessageEventDAO
{
	List<EbMSMessageEvent> getEbMSMessageEvents(EbMSMessageContext messageContext, EbMSMessageEventType[] types) throws DAOException;
	List<EbMSMessageEvent> getEbMSMessageEvents(EbMSMessageContext messageContext, EbMSMessageEventType[] types, int maxNr) throws DAOException;
	void insertEbMSMessageEvent(String messageId, EbMSMessageEventType eventType) throws DAOException;
	int processEbMSMessageEvent(String messageId) throws DAOException;
}