/*
 * Copyright 2011 Clockwork
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.clockwork.ebms.server.embedded.dao;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.StringPath;
import nl.clockwork.ebms.api.ebms.model.Party;
import nl.clockwork.ebms.querydsl.model.QEbmsMessage;
import nl.clockwork.ebms.server.embedded.web.message.EbMSMessageFilter;

public interface WithMessageFilter
{
	default BooleanBuilder applyFilter(QEbmsMessage table, EbMSMessageFilter messageContext, BooleanBuilder builder)
	{
		if (messageContext == null)
			return builder;
		addIfNotNull(messageContext.getCpaId(), builder::and, table.cpaId::eq);
		applyPathFilter(table.fromPartyId, table.fromRole, messageContext.getFromParty(), builder);
		applyPathFilter(table.toPartyId, table.toRole, messageContext.getToParty(), builder);
		addIfNotNull(messageContext.getService(), builder::and, table.service::eq);
		addIfNotNull(messageContext.getAction(), builder::and, table.action::eq);
		addIfNotNull(messageContext.getConversationId(), builder::and, table.conversationId::eq);
		addIfNotNull(messageContext.getMessageId(), builder::and, table.messageId::eq);
		addIfNotNull(messageContext.getRefToMessageId(), builder::and, table.refToMessageId::eq);
		addIfNotEmpty(messageContext.getStatuses(), builder::and, table.status::in);
		return builder;
	}

	default <T> void addIfNotNull(
			T value,
			java.util.function.Consumer<com.querydsl.core.types.Predicate> consumer,
			java.util.function.Function<T, com.querydsl.core.types.Predicate> predicateFactory)
	{
		if (value != null)
			consumer.accept(predicateFactory.apply(value));
	}

	default <T extends java.util.Collection<?>> void addIfNotEmpty(
			T value,
			java.util.function.Consumer<com.querydsl.core.types.Predicate> consumer,
			java.util.function.Function<T, com.querydsl.core.types.Predicate> predicateFactory)
	{
		if (value != null && !value.isEmpty())
			consumer.accept(predicateFactory.apply(value));
	}

	default void applyPathFilter(StringPath partyId, StringPath role, Party party, BooleanBuilder builder)
	{
		if (party != null)
		{
			builder.and(partyId.eq(party.getPartyId()));
			if (party.getRole() != null)
				builder.and(role.eq(party.getRole()));
		}
	}
}
