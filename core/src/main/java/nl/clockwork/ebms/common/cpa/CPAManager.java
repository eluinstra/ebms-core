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

import static java.util.Optional.empty;

import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import nl.clockwork.ebms.EbMSAction;
import nl.clockwork.ebms.model.EbMSPartyInfo;
import nl.clockwork.ebms.model.FromPartyInfo;
import nl.clockwork.ebms.model.Party;
import nl.clockwork.ebms.model.ToPartyInfo;
import nl.clockwork.ebms.security.EbMSKeyStore;
import nl.clockwork.ebms.util.StreamUtils;
import org.apache.commons.lang3.StringUtils;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.MessagingCharacteristics;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PartyInfo;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.SyncReplyModeType;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Service;
import org.springframework.aop.framework.AopContext;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@CacheConfig(cacheNames = {"CPAManager"})
public class CPAManager
{
	@NonNull
	CPARepository cpaRepository;
	@NonNull
	BiFunction<String, X509Certificate, X509Certificate> overrideCertificate;
	@NonNull
	Function<String, String> overrideURL;
	@NonNull
	EbMSKeyStore keyStore;
	boolean useClientCertificate;

	@Cacheable(cacheNames = "CPAManager", keyGenerator = "ebMSKeyGenerator")
	public boolean existsCPA(String cpaId)
	{
		return cpaRepository.existsCPA(cpaId);
	}

	@Cacheable(cacheNames = "CPAManager", keyGenerator = "ebMSKeyGenerator")
	public boolean existsPartyId(String cpaId, String partyId)
	{
		return cpaRepository.getCPA(cpaId).map(CPAQuery.existsPartyId(partyId)).orElse(false);
	}

	@Cacheable(cacheNames = "CPAManager", keyGenerator = "ebMSKeyGenerator")
	public Optional<EbMSPartyInfo> getEbMSPartyInfo(String cpaId, String partyId)
	{
		return cpaRepository.getCPA(cpaId).map(CPAQuery.getEbMSPartyInfo(partyId)).orElse(empty());
	}

	@Cacheable(cacheNames = "CPAManager", keyGenerator = "ebMSKeyGenerator")
	public Optional<PartyInfo> getPartyInfo(String cpaId, List<PartyId> partyId)
	{
		return cpaRepository.getCPA(cpaId).map(CPAQuery.getPartyInfo(partyId)).orElse(empty());
	}

	@Cacheable(cacheNames = "CPAManager", keyGenerator = "ebMSKeyGenerator")
	public Optional<FromPartyInfo> getFromPartyInfo(String cpaId, Party fromParty, String service, String action)
	{
		return cpaRepository.getCPA(cpaId).map(CPAQuery.getFromPartyInfo(fromParty, service, action)).orElse(empty());
	}

	@Cacheable(cacheNames = "CPAManager", keyGenerator = "ebMSKeyGenerator")
	public Optional<ToPartyInfo> getToPartyInfoByFromPartyActionBinding(String cpaId, Party fromParty, String service, String action)
	{
		return ((CPAManager)AopContext.currentProxy()).getFromPartyInfo(cpaId, fromParty, service, action)
				.flatMap(fromPartyInfo -> cpaRepository.getCPA(cpaId).map(CPAQuery.getToPartyInfoByFromPartyActionBinding(fromPartyInfo, fromParty, service, action)))
				.orElse(empty());
	}

	@Cacheable(cacheNames = "CPAManager", keyGenerator = "ebMSKeyGenerator")
	public Optional<ToPartyInfo> getToPartyInfo(String cpaId, Party toParty, String service, String action)
	{
		return cpaRepository.getCPA(cpaId).map(CPAQuery.getToPartyInfo(toParty, service, action)).orElse(empty());
	}

	@Cacheable(cacheNames = "CPAManager", keyGenerator = "ebMSKeyGenerator")
	public boolean canSend(String cpaId, List<PartyId> partyId, String role, Service service, String action)
	{
		return cpaRepository.getCPA(cpaId).map(CPAQuery.canSend(partyId, role, service, action)).orElse(false);
	}

	@Cacheable(cacheNames = "CPAManager", keyGenerator = "ebMSKeyGenerator")
	public boolean canReceive(String cpaId, List<PartyId> partyId, String role, Service service, String action)
	{
		return cpaRepository.getCPA(cpaId).map(CPAQuery.canReceive(partyId, role, service, action)).orElse(false);
	}

	@Cacheable(cacheNames = "CPAManager", keyGenerator = "ebMSKeyGenerator")
	public Optional<DeliveryChannel> getDeliveryChannel(String cpaId, String deliveryChannelId)
	{
		return cpaRepository.getCPA(cpaId).map(CPAQuery.getDeliveryChannel(deliveryChannelId)).orElse(empty());
	}

	@Cacheable(cacheNames = "CPAManager", keyGenerator = "ebMSKeyGenerator")
	public Optional<DeliveryChannel> getDefaultDeliveryChannel(String cpaId, List<PartyId> partyId, String action)
	{
		return ((CPAManager)AopContext.currentProxy()).getPartyInfo(cpaId, partyId).map(CPAQuery.getDefaultDeliveryChannel(action)).orElse(empty());
	}

	public Optional<DeliveryChannel> getSendDeliveryChannel(MessageHeader messageHeader)
	{
		return ((CPAManager)AopContext.currentProxy()).getSendDeliveryChannel(
				messageHeader.getCPAId(),
				messageHeader.getFrom().getPartyId(),
				messageHeader.getFrom().getRole(),
				messageHeader.getService(),
				messageHeader.getAction());
	}

	@Cacheable(cacheNames = "CPAManager", keyGenerator = "ebMSKeyGenerator")
	public Optional<DeliveryChannel> getSendDeliveryChannel(String cpaId, List<PartyId> partyId, String role, Service service, String action)
	{
		return service != null && EbMSAction.EBMS_SERVICE_URI.equals(service.toString())
				? ((CPAManager)AopContext.currentProxy()).getDefaultDeliveryChannel(cpaId, partyId, action)
				: ((CPAManager)AopContext.currentProxy()).getPartyInfo(cpaId, partyId).flatMap(CPAQuery.getSendDeliveryChannel(role, service, action));
	}

	@Cacheable(cacheNames = "CPAManager", keyGenerator = "ebMSKeyGenerator")
	public X509Certificate getX509Certificate(DeliveryChannel deliveryChannel)
	{
		return deliveryChannel != null ? CPAUtils.getX509Certificate(CPAUtils.getClientCertificate(deliveryChannel)) : null;
	}

	public Optional<String> getSSLClientAlias(MessageHeader messageHeader)
	{
		val deliveryChannel = ((CPAManager)AopContext.currentProxy()).getSendDeliveryChannel(messageHeader).orElse(null);
		return ((CPAManager)AopContext.currentProxy()).getSSLClientAlias(messageHeader.getCPAId(), deliveryChannel);
	}

	@Cacheable(cacheNames = "CPAManager", keyGenerator = "ebMSKeyGenerator")
	public Optional<String> getSSLClientAlias(String cpaId, DeliveryChannel deliveryChannel)
	{
		return Optional.ofNullable(deliveryChannel)
				.filter(dc -> useClientCertificate)
				.map(((CPAManager)AopContext.currentProxy())::getX509Certificate)
				.map(c -> overrideCertificate.apply(cpaId, c))
				.map(this::toCertificateAlias)
				.map(this::toDefaultAliasIfEmpty);
	}

	private String toCertificateAlias(X509Certificate c)
	{
		// TODO: improve error handling
		try
		{
			return keyStore.getCertificateAlias(c);
		}
		catch (KeyStoreException e)
		{
			log.warn("Error getting certificate alias from keystore", e);
			return null;
		}
	}

	private String toDefaultAliasIfEmpty(String clientAlias)
	{
		return clientAlias == null && StringUtils.isNotEmpty(keyStore.getDefaultAlias()) ? keyStore.getDefaultAlias() : clientAlias;
	}

	@Cacheable(cacheNames = "CPAManager", keyGenerator = "ebMSKeyGenerator")
	public Optional<DeliveryChannel> getReceiveDeliveryChannel(String cpaId, List<PartyId> partyId, String role, Service service, String action)
	{
		return service != null && EbMSAction.EBMS_SERVICE_URI.equals(service.toString())
				? ((CPAManager)AopContext.currentProxy()).getDefaultDeliveryChannel(cpaId, partyId, action)
				: ((CPAManager)AopContext.currentProxy()).getPartyInfo(cpaId, partyId).flatMap(CPAQuery.getReceiveDeliveryChannel(role, service, action));
	}

	@Cacheable(cacheNames = "CPAManager", keyGenerator = "ebMSKeyGenerator")
	public boolean isSendingNonRepudiationRequired(String cpaId, List<PartyId> partyId, String role, Service service, String action)
	{
		val deliveryChannel = ((CPAManager)AopContext.currentProxy()).getSendDeliveryChannel(cpaId, partyId, role, service, action)
				.orElseThrow(() -> StreamUtils.illegalStateException("SendDeliveryChannel", cpaId, partyId, role, service, action));
		val docExchange = CPAUtils.getDocExchange(deliveryChannel);
		return cpaRepository.getCPA(cpaId).map(CPAQuery.isSendingNonRepudiationRequired(docExchange, partyId, role, service, action)).orElse(false);
	}

	@Cacheable(cacheNames = "CPAManager", keyGenerator = "ebMSKeyGenerator")
	public boolean isSendingConfidential(String cpaId, List<PartyId> partyId, String role, Service service, String action)
	{
		val deliveryChannel = ((CPAManager)AopContext.currentProxy()).getSendDeliveryChannel(cpaId, partyId, role, service, action)
				.orElseThrow(() -> StreamUtils.illegalStateException("SendDeliveryChannel", cpaId, partyId, role, service, action));
		val docExchange = CPAUtils.getDocExchange(deliveryChannel);
		return cpaRepository.getCPA(cpaId).map(CPAQuery.isSendingConfidential(docExchange, partyId, role, service, action)).orElse(false);
	}

	public String getReceivingUri(MessageHeader messageHeader)
	{
		return ((CPAManager)AopContext.currentProxy()).getReceivingUri(
				messageHeader.getCPAId(),
				messageHeader.getTo().getPartyId(),
				messageHeader.getTo().getRole(),
				messageHeader.getService(),
				messageHeader.getAction());
	}

	@Cacheable(cacheNames = "CPAManager", keyGenerator = "ebMSKeyGenerator")
	public String getReceivingUri(String cpaId, List<PartyId> partyId, String role, Service service, String action)
	{
		val deliveryChannel = ((CPAManager)AopContext.currentProxy()).getReceiveDeliveryChannel(cpaId, partyId, role, service, action)
				.orElseThrow(() -> StreamUtils.illegalStateException("ReceiveDeliveryChannel", cpaId, partyId, role, service, action));
		return overrideURL.apply(CPAUtils.getUri(deliveryChannel));
	}

	@Cacheable(cacheNames = "CPAManager", keyGenerator = "ebMSKeyGenerator")
	public Optional<SyncReplyModeType> getSendSyncReply(String cpaId, List<PartyId> partyId, String role, Service service, String action)
	{
		return ((CPAManager)AopContext.currentProxy()).getSendDeliveryChannel(cpaId, partyId, role, service, action)
				.map(DeliveryChannel::getMessagingCharacteristics)
				.map(MessagingCharacteristics::getSyncReplyMode);
	}
}
