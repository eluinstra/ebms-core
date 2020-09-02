package nl.clockwork.ebms.service.model;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MessageMapper
{
	public MessageMapper INSTANCE = Mappers.getMapper(MessageMapper.class);

	MessageRequest toMessage(Message message);
}
