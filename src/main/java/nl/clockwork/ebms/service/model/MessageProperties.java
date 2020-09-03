package nl.clockwork.ebms.service.model;

import java.io.Serializable;
import java.time.Instant;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.EbMSMessageStatus;
import nl.clockwork.ebms.jaxb.InstantAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class MessageProperties implements Serializable
{
	private static final long serialVersionUID = 1L;
	@XmlElement(required = true)
	@NonNull
	String cpaId;
	@XmlElement(required = true)
	@NonNull
	Party fromParty;
	@XmlElement(required = true)
	@NonNull
	Party toParty;
	@XmlElement(required = true)
	@NonNull
	String service;
	@XmlElement(required = true)
	@NonNull
	String action;
	@XmlElement(required = true)
	@XmlJavaTypeAdapter(InstantAdapter.class)
	@XmlSchemaType(name="dateTime")
	@NonNull
	Instant timestamp;
	@XmlElement(required = true)
	@NonNull
	String conversationId;
	@XmlElement(required = true)
	@NonNull
	String messageId;
	String refToMessageId;
	@XmlElement(required = true)
	@NonNull
	EbMSMessageStatus messageStatus;

	public MessageProperties(String cpaId, String fromPartyId, String fromRole, String toPartyId, String toRole, String service, String action, Instant timestamp, String conversationId, String messageId, String refToMessageId, EbMSMessageStatus messageStatus)
	{
		this.cpaId = cpaId;
		this.fromParty = new Party(fromPartyId,fromRole);
		this.toParty = new Party(toPartyId,toRole);
		this.service = service;
		this.action = action;
		this.timestamp = timestamp;
		this.conversationId = conversationId;
		this.messageId = messageId;
		this.refToMessageId = refToMessageId;
		this.messageStatus = messageStatus;
	}
}
