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
package nl.clockwork.ebms.common.cpa;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;
import nl.clockwork.ebms.model.EbMSPartyInfo;
import nl.clockwork.ebms.model.FromPartyInfo;
import nl.clockwork.ebms.model.Party;
import nl.clockwork.ebms.model.ToPartyInfo;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PartyInfo;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.SyncReplyModeType;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Service;

public interface CPAManagerCacheInterface
{

	void setSelf(CPAManagerCacheInterface self);

	boolean existsCPA(String cpaId);

	boolean existsPartyId(String cpaId, String partyId);

	Optional<EbMSPartyInfo> getEbMSPartyInfo(String cpaId, String partyId);

	Optional<PartyInfo> getPartyInfo(String cpaId, List<PartyId> partyId);

	Optional<FromPartyInfo> getFromPartyInfo(String cpaId, Party fromParty, String service, String action);

	Optional<ToPartyInfo> getToPartyInfoByFromPartyActionBinding(String cpaId, Party fromParty, String service, String action);

	Optional<ToPartyInfo> getToPartyInfo(String cpaId, Party toParty, String service, String action);

	boolean canSend(String cpaId, List<PartyId> partyId, String role, Service service, String action);

	boolean canReceive(String cpaId, List<PartyId> partyId, String role, Service service, String action);

	Optional<DeliveryChannel> getDeliveryChannel(String cpaId, String deliveryChannelId);

	Optional<DeliveryChannel> getDefaultDeliveryChannel(String cpaId, List<PartyId> partyId, String action);

	Optional<DeliveryChannel> getSendDeliveryChannel(MessageHeader messageHeader);

	Optional<DeliveryChannel> getSendDeliveryChannel(String cpaId, List<PartyId> partyId, String role, Service service, String action);

	X509Certificate getX509Certificate(DeliveryChannel deliveryChannel);

	Optional<String> getSSLClientAlias(MessageHeader messageHeader);

	Optional<String> getSSLClientAlias(String cpaId, DeliveryChannel deliveryChannel);

	Optional<DeliveryChannel> getReceiveDeliveryChannel(String cpaId, List<PartyId> partyId, String role, Service service, String action);

	boolean isSendingNonRepudiationRequired(String cpaId, List<PartyId> partyId, String role, Service service, String action);

	boolean isSendingConfidential(String cpaId, List<PartyId> partyId, String role, Service service, String action);

	String getReceivingUri(MessageHeader messageHeader);

	String getReceivingUri(String cpaId, List<PartyId> partyId, String role, Service service, String action);

	Optional<SyncReplyModeType> getSendSyncReply(String cpaId, List<PartyId> partyId, String role, Service service, String action);

}