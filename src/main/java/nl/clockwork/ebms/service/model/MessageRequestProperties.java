package nl.clockwork.ebms.service.model;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
//@RequiredArgsConstructor
@AllArgsConstructor
public class MessageRequestProperties implements Serializable
{
	private static final long serialVersionUID = 1L;
	@XmlElement(required=true)
	@NonNull
	String cpaId;
	@XmlElement(required=true)
	@NonNull
	Party fromParty;
	Party toParty;
	@XmlElement(required=true)
	//@NonNull
	String service;
	@XmlElement(required=true)
	//@NonNull
	String action;
	String conversationId;
	String messageId;
	String refToMessageId;

	public MessageRequestProperties(@NonNull String cpaId, @NonNull Party fromParty, @NonNull String service, @NonNull String action)
	{
		super();
		this.cpaId = cpaId;
		this.fromParty = fromParty;
		this.service = service;
		this.action = action;
	}
}
