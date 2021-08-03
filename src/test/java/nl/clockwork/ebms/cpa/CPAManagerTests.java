/**
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
package nl.clockwork.ebms.cpa;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static nl.clockwork.ebms.cpa.CPATestUtils.cpaCache;
import static nl.clockwork.ebms.cpa.CPATestUtils.loadCPA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.xml.bind.JAXBException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ServiceType;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.SyncReplyModeType;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId;

import lombok.val;
import nl.clockwork.ebms.cpa.url.URLMapper;
import nl.clockwork.ebms.cpa.url.URLMappingDAO;
import nl.clockwork.ebms.model.EbMSPartyInfo;
import nl.clockwork.ebms.model.Party;

@TestInstance(value = Lifecycle.PER_CLASS)
public class CPAManagerTests
{
	private static final String DEFAULT_CPA_ID = "cpaStubEBF.rm.https.signed";
	private static final String ENCRYPTED_CPA_ID = "cpaStubEBF.rm.https.signed.encrypted";
	private static final String SYNC_CPA_ID = "cpaStubEBF.rm.https.signed.sync";
	private static final String NOT_EXISTING_CPA_ID = "cpaStubEBF.rm.https.signed.not.existing";

  private static final String DEFAULT_PARTY_ID_TYPE = "urn:osb:oin";
  private static final PartyId INVALID_PARTY_ID = createPartyId(DEFAULT_PARTY_ID_TYPE,"00000000000000000002");
  private static final String RANDOM = "xxx";

  private static final ServiceType DEFAULT_SERVICE = createDefaultService();
  private static final String MESSAGE_ERROR_ACTION = "MessageError";
  private static final String ACKNOWLEDGMENT_ACTION = "Acknowledgment";

  private static final String DIGIPOORT_PARTY_NAME = "Logius";
  private static final String DIGIPOORT_PARTY_ID_VALUE = "00000000000000000000";
  private static final PartyId DIGIPOORT_PARTY_ID = createPartyId(DEFAULT_PARTY_ID_TYPE,DIGIPOORT_PARTY_ID_VALUE);
  private static final String DIGIPOORT_ROLE = "DIGIPOORT";
  private static final String DIGIPOORT_DEFAULT_DELIVERY_CHANNEL = "DIGIPOORT_defaultDeliveryChannel_ProfileBestEffortSigned";
  private static final String DIGIPOORT_RELIABLE_DELIVERY_CHANNEL = "DIGIPOORT_defaultDeliveryChannel_ProfileReliableMessagingSigned";
  private static final String DIGIPOORT_URL = "https://localhost:8888/ebms";


  private static final String OVERHEID_PARTY_NAME = "Overheid";
  private static final PartyId OVERHEID_PARTY_ID = createPartyId(DEFAULT_PARTY_ID_TYPE,"00000000000000000001");
  private static final String OVERHEID_ROLE = "OVERHEID";
  private static final String OVERHEID_DEFAULT_DELIVERY_CHANNEL = "OVERHEID_defaultDeliveryChannel_ProfileBestEffortSigned";
  private static final String OVERHEID_RELIABLE_DELIVERY_CHANNEL = "OVERHEID_defaultDeliveryChannel_ProfileReliableMessagingSigned";
  private static final String OVERHEID_URL = "https://localhost:8088/ebms";

  private static final ServiceType AANLEVEREN_SERVICE = createService("urn:osb:services","osb:aanleveren:1.1$1.0");
  private static final String AANLEVEREN_ACTION = "aanleveren";
  private static final String BEVESTIG_AANLEVEREN_ACTION = "bevestigAanleveren";

  private static final ServiceType AFLEVEREN_SERVICE = createService("urn:osb:services","osb:afleveren:1.1$1.0");
  private static final String AFLEVEREN_ACTION = "afleveren";
  private static final String BEVESTIG_AFLEVEREN_ACTION = "bevestigAfleveren";

  @Mock
  CPADAO cpaDAO;
  @Mock
  URLMappingDAO urlMappingDAO;
  URLMapper urlMapper;
  CPAManager cpaManager;

  @BeforeAll
  void init() throws IOException, JAXBException
  {
		MockitoAnnotations.openMocks(this);
    when(cpaDAO.existsCPA(DEFAULT_CPA_ID)).thenReturn(true);
    when(cpaDAO.existsCPA(ENCRYPTED_CPA_ID)).thenReturn(true);
    when(cpaDAO.existsCPA(SYNC_CPA_ID)).thenReturn(true);
    when(cpaDAO.getCPA(DEFAULT_CPA_ID)).thenReturn(loadCPA(DEFAULT_CPA_ID));
    when(cpaDAO.getCPA(ENCRYPTED_CPA_ID)).thenReturn(loadCPA(ENCRYPTED_CPA_ID));
    when(cpaDAO.getCPA(SYNC_CPA_ID)).thenReturn(loadCPA(SYNC_CPA_ID));
    when(urlMappingDAO.getURLMapping(anyString())).thenReturn(Optional.empty());
    urlMapper = new URLMapper(urlMappingDAO);
    cpaManager = new CPAManager(cpaDAO, urlMapper);
  }

  private static ServiceType createDefaultService()
  {
    return createService(null,"urn:oasis:names:tc:ebxml-msg:service");
  }

  @Test
  void existsCPA()
  {
    assertThat(cpaManager.existsCPA(DEFAULT_CPA_ID)).isTrue();
  }

  @Test
  void notExistsCPA()
  {
    assertThat(cpaManager.existsCPA(NOT_EXISTING_CPA_ID)).isFalse();
  }

  @Test
  void getCPA()
  {
    assertThat(cpaManager.getCPA(DEFAULT_CPA_ID).get().getCpaid()).isEqualTo(cpaCache.apply(DEFAULT_CPA_ID).get().getCpaid());
  }

  @ParameterizedTest
  @ValueSource(strings = {"2011-01-01T00:00:00Z","2020-01-01T00:00:00.00Z","2021-01-01T00:00:00Z"})
  void isValid(String timestamp)
  {
    assertThat(cpaManager.isValid(DEFAULT_CPA_ID,Instant.parse(timestamp))).isTrue();
  }

  @ParameterizedTest
  @MethodSource
  void isNotValid(String cpaId, String timestamp)
  {
    assertThat(cpaManager.isValid(cpaId,Instant.parse(timestamp))).isFalse();
  }

  private static Stream<Arguments> isNotValid()
  {
    return Stream.of(
      arguments(DEFAULT_CPA_ID,"2010-12-31T23:59:59Z"),
      arguments(DEFAULT_CPA_ID,"2021-01-01T00:00:01Z"),
      arguments(NOT_EXISTING_CPA_ID,"2020-01-01T00:00:00.00Z"));
  }

  @ParameterizedTest
  @MethodSource
  void existsPartyId(String partyId)
  {
    assertThat(cpaManager.existsPartyId(DEFAULT_CPA_ID,partyId)).isTrue();
  }

  private static Stream<Arguments> existsPartyId()
  {
    return Stream.of(
        arguments(CPAUtils.toString(DIGIPOORT_PARTY_ID)),
        arguments(CPAUtils.toString(OVERHEID_PARTY_ID)));
  }

  @ParameterizedTest
  @MethodSource
  void notExistsPartyId(String cpaId, String partyId)
  {
    assertThat(cpaManager.existsPartyId(cpaId,partyId)).isFalse();
  }

  private static Stream<Arguments> notExistsPartyId()
  {
    return Stream.of(
        arguments(DEFAULT_CPA_ID,null),
        arguments(DEFAULT_CPA_ID,""),
        arguments(DEFAULT_CPA_ID,CPAUtils.toString(INVALID_PARTY_ID)),
        arguments(NOT_EXISTING_CPA_ID,""));
  }

  @ParameterizedTest
  @MethodSource
  void getEbMSPartyInfo(String cpaId, String partyId, EbMSPartyInfo expectedEbMSPartyInfo)
  {
    assertThat(cpaManager.getEbMSPartyInfo(cpaId,partyId))
        .hasValueSatisfying(partyInfo -> 
        {
          assertThat(partyInfo.getPartyIds().size()).isEqualTo(1);
          assertThat(partyInfo.getPartyIds().get(0)).satisfies(id ->
          {
            assertThat(id.getType()).isEqualTo(expectedEbMSPartyInfo.getPartyIds().get(0).getType());
            assertThat(id.getValue()).isEqualTo(expectedEbMSPartyInfo.getPartyIds().get(0).getValue());
          });
          assertThat(partyInfo.getRole()).isEqualTo(expectedEbMSPartyInfo.getRole());
        });
  }

  private static Stream<Arguments> getEbMSPartyInfo()
  {
    return Stream.of(
        arguments(DEFAULT_CPA_ID,CPAUtils.toString(DIGIPOORT_PARTY_ID),createEbMSPartyInfo(DIGIPOORT_PARTY_ID,null)),
        arguments(DEFAULT_CPA_ID,CPAUtils.toString(OVERHEID_PARTY_ID),createEbMSPartyInfo(OVERHEID_PARTY_ID,null)));
  }

  private static EbMSPartyInfo createEbMSPartyInfo(PartyId partyId, String role) {
    return new EbMSPartyInfo(asList(partyId),role);
  }

  @ParameterizedTest
  @MethodSource
  void getNoEbMSPartyInfo(String cpaId, String partyId)
  {
    assertThat(cpaManager.getEbMSPartyInfo(cpaId,partyId)).isEmpty();
  }

  private static Stream<Arguments> getNoEbMSPartyInfo()
  {
    return Stream.of(
        arguments(DEFAULT_CPA_ID,null),
        arguments(DEFAULT_CPA_ID,""),
        arguments(DEFAULT_CPA_ID,CPAUtils.toString(INVALID_PARTY_ID)),
        arguments(NOT_EXISTING_CPA_ID,""));
  }

  @ParameterizedTest
  @MethodSource
  void getPartyInfo(String cpaId, List<PartyId> partyIds, String expectedPartyName)
  {
    assertThat(cpaManager.getPartyInfo(cpaId,partyIds))
        .hasValueSatisfying(partyInfo -> assertThat(partyInfo.getPartyName()).isEqualTo(expectedPartyName));
  }

  private static Stream<Arguments> getPartyInfo()
  {
    return Stream.of(
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_PARTY_NAME),
        arguments(DEFAULT_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_PARTY_NAME));
  }

  private static PartyId createPartyId(String type, String value) {
    val result = new PartyId();
    result.setType(type);
    result.setValue(value);
    return result;
  }

  @ParameterizedTest
  @MethodSource
  void getNoPartyInfo(String cpaId, List<PartyId> partyIds)
  {
    assertThat(cpaManager.getPartyInfo(cpaId,partyIds))
        .isEmpty();
  }

  private static Stream<Arguments> getNoPartyInfo()
  {
    return Stream.of(
        arguments(DEFAULT_CPA_ID,asList(createPartyId(null,DIGIPOORT_PARTY_ID_VALUE))),
        arguments(DEFAULT_CPA_ID,asList(createPartyId(DEFAULT_PARTY_ID_TYPE,null))),
        arguments(DEFAULT_CPA_ID,asList(createPartyId("",DIGIPOORT_PARTY_ID_VALUE))),
        arguments(DEFAULT_CPA_ID,asList(createPartyId(DEFAULT_PARTY_ID_TYPE,""))),
        arguments(DEFAULT_CPA_ID,asList(createPartyId(DEFAULT_PARTY_ID_TYPE + RANDOM,DIGIPOORT_PARTY_ID_VALUE))),
        arguments(DEFAULT_CPA_ID,asList(INVALID_PARTY_ID)),
        arguments(DEFAULT_CPA_ID,emptyList()),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID,DIGIPOORT_PARTY_ID)),
        arguments(NOT_EXISTING_CPA_ID,asList(DIGIPOORT_PARTY_ID)));
  }

  @ParameterizedTest
  @MethodSource
  void getFromPartyInfo(String cpaId, Party fromParty, String service, String action)
  {
    assertThat(cpaManager.getFromPartyInfo(cpaId, fromParty, service, action))
        .hasValueSatisfying(fromPartyInfo -> {/* TODO */});
  }

  private static Stream<Arguments> getFromPartyInfo()
  {
    return Stream.of(
      arguments(DEFAULT_CPA_ID,Party.of(CPAUtils.toString(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE),CPAUtils.toString(AFLEVEREN_SERVICE),AFLEVEREN_ACTION),
      arguments(DEFAULT_CPA_ID,Party.of(CPAUtils.toString(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE),CPAUtils.toString(AANLEVEREN_SERVICE),BEVESTIG_AANLEVEREN_ACTION),
      arguments(DEFAULT_CPA_ID,Party.of(CPAUtils.toString(OVERHEID_PARTY_ID),OVERHEID_ROLE),CPAUtils.toString(AFLEVEREN_SERVICE),BEVESTIG_AFLEVEREN_ACTION),
      arguments(DEFAULT_CPA_ID,Party.of(CPAUtils.toString(OVERHEID_PARTY_ID),OVERHEID_ROLE),CPAUtils.toString(AANLEVEREN_SERVICE),AANLEVEREN_ACTION),
      arguments(DEFAULT_CPA_ID,null,CPAUtils.toString(AFLEVEREN_SERVICE),BEVESTIG_AFLEVEREN_ACTION));
  }

  private static ServiceType createService(String type, String value)
  {
    val result = new ServiceType();
    result.setType(type);
    result.setValue(value);
    return result;
  }

  @ParameterizedTest
  @MethodSource
  void getNoFromPartyInfo(String cpaId, Party fromParty, String service, String action)
  {
    assertThat(cpaManager.getFromPartyInfo(cpaId, fromParty, service, action)).isEmpty();
  }

  private static Stream<Arguments> getNoFromPartyInfo()
  {
    return Stream.of(
      arguments(DEFAULT_CPA_ID,Party.of(CPAUtils.toString(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE),CPAUtils.toString(AFLEVEREN_SERVICE),BEVESTIG_AFLEVEREN_ACTION),
      arguments(DEFAULT_CPA_ID,Party.of(CPAUtils.toString(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE),CPAUtils.toString(AANLEVEREN_SERVICE),AANLEVEREN_ACTION),
      arguments(DEFAULT_CPA_ID,Party.of(CPAUtils.toString(OVERHEID_PARTY_ID),OVERHEID_ROLE),CPAUtils.toString(AFLEVEREN_SERVICE),AFLEVEREN_ACTION),
      arguments(DEFAULT_CPA_ID,Party.of(CPAUtils.toString(OVERHEID_PARTY_ID),OVERHEID_ROLE),CPAUtils.toString(AANLEVEREN_SERVICE),BEVESTIG_AANLEVEREN_ACTION),
      arguments(DEFAULT_CPA_ID,Party.of(CPAUtils.toString(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE),null,BEVESTIG_AFLEVEREN_ACTION),
      arguments(DEFAULT_CPA_ID,Party.of(CPAUtils.toString(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE),CPAUtils.toString(AFLEVEREN_SERVICE),null));
  }

  @ParameterizedTest
  @MethodSource
  void getToPartyInfoByFromPartyActionBinding(String cpaId, Party fromParty, String service, String action)
  {
    assertThat(cpaManager.getToPartyInfoByFromPartyActionBinding(cpaId, fromParty, service, action))
        .hasValueSatisfying(fromPartyInfo -> {/* TODO */});
  }

  private static Stream<Arguments> getToPartyInfoByFromPartyActionBinding()
  {
    return Stream.of(
      arguments(DEFAULT_CPA_ID,Party.of(CPAUtils.toString(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE),CPAUtils.toString(AFLEVEREN_SERVICE),AFLEVEREN_ACTION),
      arguments(DEFAULT_CPA_ID,Party.of(CPAUtils.toString(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE),CPAUtils.toString(AANLEVEREN_SERVICE),BEVESTIG_AANLEVEREN_ACTION),
      arguments(DEFAULT_CPA_ID,Party.of(CPAUtils.toString(OVERHEID_PARTY_ID),OVERHEID_ROLE),CPAUtils.toString(AFLEVEREN_SERVICE),BEVESTIG_AFLEVEREN_ACTION),
      arguments(DEFAULT_CPA_ID,Party.of(CPAUtils.toString(OVERHEID_PARTY_ID),OVERHEID_ROLE),CPAUtils.toString(AANLEVEREN_SERVICE),AANLEVEREN_ACTION),
      arguments(DEFAULT_CPA_ID,null,CPAUtils.toString(AFLEVEREN_SERVICE),BEVESTIG_AFLEVEREN_ACTION));
  }

  @ParameterizedTest
  @MethodSource
  void getNoToPartyInfoByFromPartyActionBinding(String cpaId, Party fromParty, String service, String action)
  {
    assertThat(cpaManager.getToPartyInfoByFromPartyActionBinding(cpaId, fromParty, service, action)).isEmpty();
  }

  private static Stream<Arguments> getNoToPartyInfoByFromPartyActionBinding()
  {
    return Stream.of(
      arguments(DEFAULT_CPA_ID,Party.of(CPAUtils.toString(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE),CPAUtils.toString(AFLEVEREN_SERVICE),BEVESTIG_AFLEVEREN_ACTION),
      arguments(DEFAULT_CPA_ID,Party.of(CPAUtils.toString(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE),CPAUtils.toString(AANLEVEREN_SERVICE),AANLEVEREN_ACTION),
      arguments(DEFAULT_CPA_ID,Party.of(CPAUtils.toString(OVERHEID_PARTY_ID),OVERHEID_ROLE),CPAUtils.toString(AFLEVEREN_SERVICE),AFLEVEREN_ACTION),
      arguments(DEFAULT_CPA_ID,Party.of(CPAUtils.toString(OVERHEID_PARTY_ID),OVERHEID_ROLE),CPAUtils.toString(AANLEVEREN_SERVICE),BEVESTIG_AANLEVEREN_ACTION),
      arguments(DEFAULT_CPA_ID,Party.of(CPAUtils.toString(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE),null,BEVESTIG_AFLEVEREN_ACTION),
      arguments(DEFAULT_CPA_ID,Party.of(CPAUtils.toString(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE),CPAUtils.toString(AFLEVEREN_SERVICE),null));
  }

  @ParameterizedTest
  @MethodSource
  void getToPartyInfo(String cpaId, Party fromParty, String service, String action)
  {
    assertThat(cpaManager.getToPartyInfo(cpaId, fromParty, service, action))
        .hasValueSatisfying(fromPartyInfo -> {/* TODO */});
  }

  private static Stream<Arguments> getToPartyInfo()
  {
    return Stream.of(
      arguments(DEFAULT_CPA_ID,Party.of(CPAUtils.toString(OVERHEID_PARTY_ID),OVERHEID_ROLE),CPAUtils.toString(AFLEVEREN_SERVICE),AFLEVEREN_ACTION),
      arguments(DEFAULT_CPA_ID,Party.of(CPAUtils.toString(OVERHEID_PARTY_ID),OVERHEID_ROLE),CPAUtils.toString(AANLEVEREN_SERVICE),BEVESTIG_AANLEVEREN_ACTION),
      arguments(DEFAULT_CPA_ID,Party.of(CPAUtils.toString(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE),CPAUtils.toString(AFLEVEREN_SERVICE),BEVESTIG_AFLEVEREN_ACTION),
      arguments(DEFAULT_CPA_ID,Party.of(CPAUtils.toString(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE),CPAUtils.toString(AANLEVEREN_SERVICE),AANLEVEREN_ACTION),
      arguments(DEFAULT_CPA_ID,null,CPAUtils.toString(AFLEVEREN_SERVICE),BEVESTIG_AFLEVEREN_ACTION));
  }

  @ParameterizedTest
  @MethodSource
  void getNoToPartyInfo(String cpaId, Party fromParty, String service, String action)
  {
    assertThat(cpaManager.getToPartyInfo(cpaId, fromParty, service, action)).isEmpty();
  }

  private static Stream<Arguments> getNoToPartyInfo()
  {
    return Stream.of(
      arguments(DEFAULT_CPA_ID,Party.of(CPAUtils.toString(OVERHEID_PARTY_ID),OVERHEID_ROLE),CPAUtils.toString(AFLEVEREN_SERVICE),BEVESTIG_AFLEVEREN_ACTION),
      arguments(DEFAULT_CPA_ID,Party.of(CPAUtils.toString(OVERHEID_PARTY_ID),OVERHEID_ROLE),CPAUtils.toString(AANLEVEREN_SERVICE),AANLEVEREN_ACTION),
      arguments(DEFAULT_CPA_ID,Party.of(CPAUtils.toString(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE),CPAUtils.toString(AFLEVEREN_SERVICE),AFLEVEREN_ACTION),
      arguments(DEFAULT_CPA_ID,Party.of(CPAUtils.toString(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE),CPAUtils.toString(AANLEVEREN_SERVICE),BEVESTIG_AANLEVEREN_ACTION),
      // arguments(DEFAULT_CPA_ID,Party.of(CPAUtils.toString(OVERHEID_PARTY_ID),OVERHEID_ROLE),null,BEVESTIG_AFLEVEREN_ACTION),
      arguments(DEFAULT_CPA_ID,Party.of(CPAUtils.toString(OVERHEID_PARTY_ID),OVERHEID_ROLE),CPAUtils.toString(AFLEVEREN_SERVICE),null));
  }

  @ParameterizedTest
  @MethodSource
  void canSend(String cpaId, List<PartyId> partyId, String role, String service, String action)
  {
    assertThat(cpaManager.canSend(cpaId,partyId,role,service,action)).isTrue();
  }

  private static Stream<Arguments> canSend()
  {
    return Stream.of(
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),AFLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),BEVESTIG_AANLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),BEVESTIG_AFLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),AANLEVEREN_ACTION));
  }

  @ParameterizedTest
  @MethodSource
  void canNotSend(String cpaId, List<PartyId> partyId, String role, String service, String action)
  {
    assertThat(cpaManager.canSend(cpaId,partyId,role,service,action)).isFalse();
  }

  private static Stream<Arguments> canNotSend()
  {
    return Stream.of(
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),AANLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),BEVESTIG_AFLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),BEVESTIG_AANLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),AFLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID,DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),AFLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,emptyList(),DIGIPOORT_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),AFLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(createPartyId(null,DIGIPOORT_PARTY_ID_VALUE)),DIGIPOORT_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),AFLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(createPartyId(DEFAULT_PARTY_ID_TYPE,null)),DIGIPOORT_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),AFLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),null,CPAUtils.toString(AFLEVEREN_SERVICE),AFLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,null,AFLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),null),
        arguments(NOT_EXISTING_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),AFLEVEREN_ACTION));
  }

  @ParameterizedTest
  @MethodSource
  void canReceive(String cpaId, List<PartyId> partyId, String role, String service, String action)
  {
    assertThat(cpaManager.canReceive(cpaId,partyId,role,service,action)).isTrue();
  }

  private static Stream<Arguments> canReceive()
  {
    return Stream.of(
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),AANLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),BEVESTIG_AFLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),BEVESTIG_AANLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),AFLEVEREN_ACTION));
  }

  @ParameterizedTest
  @MethodSource
  void canNotReceive(String cpaId, List<PartyId> partyId, String role, String service, String action)
  {
    assertThat(cpaManager.canReceive(cpaId,partyId,role,service,action)).isFalse();
  }

  private static Stream<Arguments> canNotReceive()
  {
    return Stream.of(
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),AFLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),BEVESTIG_AANLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),BEVESTIG_AFLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),AANLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID,DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),AFLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,emptyList(),null,null,null),
        arguments(DEFAULT_CPA_ID,emptyList(),DIGIPOORT_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),AANLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(createPartyId(null,DIGIPOORT_PARTY_ID_VALUE)),DIGIPOORT_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),AANLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(createPartyId(DEFAULT_PARTY_ID_TYPE,null)),DIGIPOORT_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),AANLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),null,CPAUtils.toString(AANLEVEREN_SERVICE),AANLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,null,AANLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),null),
        arguments(NOT_EXISTING_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),AANLEVEREN_ACTION));
  }

  @ParameterizedTest
  @MethodSource
  void getDeliveryChannel(String cpaId, String deliveryChannelId)
  {
    assertThat(cpaManager.getDeliveryChannel(cpaId,deliveryChannelId))
        .hasValueSatisfying(deliveryChannel -> assertThat(deliveryChannel.getChannelId()).isEqualTo(deliveryChannelId));
  }

  private static Stream<Arguments> getDeliveryChannel()
  {
    return Stream.of(
        arguments(DEFAULT_CPA_ID,DIGIPOORT_DEFAULT_DELIVERY_CHANNEL),
        arguments(DEFAULT_CPA_ID,DIGIPOORT_RELIABLE_DELIVERY_CHANNEL),
        arguments(DEFAULT_CPA_ID,OVERHEID_DEFAULT_DELIVERY_CHANNEL),
        arguments(DEFAULT_CPA_ID,OVERHEID_RELIABLE_DELIVERY_CHANNEL));
  }

  @ParameterizedTest
  @MethodSource
  void getNoDeliveryChannel(String cpaId, String deliveryChannelId)
  {
    assertThat(cpaManager.getDeliveryChannel(cpaId,deliveryChannelId)).isEmpty();
  }

  private static Stream<Arguments> getNoDeliveryChannel()
  {
    return Stream.of(
        arguments(DEFAULT_CPA_ID,null),
        arguments(DEFAULT_CPA_ID,""),
        arguments(DEFAULT_CPA_ID,DIGIPOORT_DEFAULT_DELIVERY_CHANNEL + RANDOM));
  }

  @ParameterizedTest
  @MethodSource
  void getDefaultDeliveryChannel(String cpaId, List<PartyId> partyId, String action, String deliveryChannelId)
  {
    assertThat(cpaManager.getDefaultDeliveryChannel(cpaId,partyId,action))
        .hasValueSatisfying(deliveryChannel -> assertThat(deliveryChannel.getChannelId()).isEqualTo(deliveryChannelId));
  }

  private static Stream<Arguments> getDefaultDeliveryChannel()
  {
    return Stream.of(
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),AFLEVEREN_ACTION,DIGIPOORT_DEFAULT_DELIVERY_CHANNEL),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),AANLEVEREN_ACTION,DIGIPOORT_DEFAULT_DELIVERY_CHANNEL),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),ACKNOWLEDGMENT_ACTION,DIGIPOORT_DEFAULT_DELIVERY_CHANNEL),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),AFLEVEREN_ACTION + RANDOM,DIGIPOORT_DEFAULT_DELIVERY_CHANNEL),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),null,DIGIPOORT_DEFAULT_DELIVERY_CHANNEL),
        arguments(DEFAULT_CPA_ID,asList(OVERHEID_PARTY_ID),AANLEVEREN_ACTION,OVERHEID_DEFAULT_DELIVERY_CHANNEL),
        arguments(DEFAULT_CPA_ID,asList(OVERHEID_PARTY_ID),AFLEVEREN_ACTION,OVERHEID_DEFAULT_DELIVERY_CHANNEL));
  }

  @ParameterizedTest
  @MethodSource
  void getNoDefaultDeliveryChannel(String cpaId, List<PartyId> partyId, String action)
  {
    assertThat(cpaManager.getDefaultDeliveryChannel(cpaId,partyId,action)).isEmpty();
  }

  private static Stream<Arguments> getNoDefaultDeliveryChannel()
  {
    return Stream.of(
        arguments(DEFAULT_CPA_ID,emptyList(),AANLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID,DIGIPOORT_PARTY_ID),AANLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(createPartyId(null,DIGIPOORT_PARTY_ID_VALUE)),AANLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(createPartyId(DEFAULT_PARTY_ID_TYPE,null)),AANLEVEREN_ACTION));
  }

  @ParameterizedTest
  @MethodSource
  void getSendDeliveryChannel(String cpaId, List<PartyId> partyId, String role, String service, String action)
  {
    assertThat(cpaManager.getSendDeliveryChannel(cpaId, partyId, role, service, action))
        .hasValueSatisfying(deliveryChannel -> {/* TODO */});
  }

  public static Stream<Arguments> getSendDeliveryChannel()
  {
    return Stream.of(
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),AFLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),BEVESTIG_AANLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(DEFAULT_SERVICE),ACKNOWLEDGMENT_ACTION),
        arguments(DEFAULT_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),BEVESTIG_AFLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),AANLEVEREN_ACTION));
  }

  @ParameterizedTest
  @MethodSource
  void getNoSendDeliveryChannel(String cpaId, List<PartyId> partyId, String role, String service, String action)
  {
    assertThat(cpaManager.getSendDeliveryChannel(cpaId, partyId, role, service, action)).isEmpty();
  }

  public static Stream<Arguments> getNoSendDeliveryChannel()
  {
    return Stream.of(
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),BEVESTIG_AFLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),AANLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),AFLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),BEVESTIG_AANLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,emptyList(),DIGIPOORT_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),AANLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),null,CPAUtils.toString(AANLEVEREN_SERVICE),AANLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,null,AANLEVEREN_ACTION),
        // FIXME: arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(DEFAULT_SERVICE),null),
        arguments(NOT_EXISTING_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),BEVESTIG_AFLEVEREN_ACTION));
  }

  @ParameterizedTest
  @MethodSource
  void getReceiveDeliveryChannel(String cpaId, List<PartyId> partyId, String role, String service, String action)
  {
    assertThat(cpaManager.getReceiveDeliveryChannel(cpaId, partyId, role, service, action))
        .hasValueSatisfying(deliveryChannel -> {/* TODO */});
  }

  public static Stream<Arguments> getReceiveDeliveryChannel()
  {
    return Stream.of(
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),AANLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),BEVESTIG_AFLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(DEFAULT_SERVICE),ACKNOWLEDGMENT_ACTION),
        arguments(DEFAULT_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),BEVESTIG_AANLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),AFLEVEREN_ACTION));
  }

  @ParameterizedTest
  @MethodSource
  void getNoReceiveDeliveryChannel(String cpaId, List<PartyId> partyId, String role, String service, String action)
  {
    assertThat(cpaManager.getReceiveDeliveryChannel(cpaId, partyId, role, service, action)).isEmpty();
  }

  private static Stream<Arguments> getNoReceiveDeliveryChannel()
  {
    return Stream.of(
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),BEVESTIG_AANLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),AFLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),AANLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),BEVESTIG_AFLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,emptyList(),DIGIPOORT_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),AANLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),null,CPAUtils.toString(AANLEVEREN_SERVICE),AANLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,null,AANLEVEREN_ACTION),
        // FIXME: arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(DEFAULT_SERVICE),null),
        arguments(NOT_EXISTING_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),BEVESTIG_AFLEVEREN_ACTION));
  }

  @ParameterizedTest
  @MethodSource
  void isSendingNonRepudiationRequired(String cpaId, List<PartyId> partyId, String role, String service, String action)
  {
    assertThat(cpaManager.isSendingNonRepudiationRequired(cpaId, partyId, role, service, action)).isTrue();
  }

  private static Stream<Arguments> isSendingNonRepudiationRequired()
  {
    return Stream.of(
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),AFLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),BEVESTIG_AANLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),BEVESTIG_AFLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),AANLEVEREN_ACTION));
  }

  @ParameterizedTest
  @MethodSource
  void isSendingNotNonRepudiationRequired(String cpaId, List<PartyId> partyId, String role, String service, String action)
  {
    assertThat(cpaManager.isSendingNonRepudiationRequired(cpaId, partyId, role, service, action)).isFalse();
  }

  private static Stream<Arguments> isSendingNotNonRepudiationRequired()
  {
    return Stream.of(
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(DEFAULT_SERVICE),MESSAGE_ERROR_ACTION),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(DEFAULT_SERVICE),ACKNOWLEDGMENT_ACTION));
  }

  @ParameterizedTest
  @MethodSource
  void isSendingConfidential(String cpaId, List<PartyId> partyId, String role, String service, String action)
  {
    assertThat(cpaManager.isSendingConfidential(cpaId, partyId, role, service, action)).isTrue();
  }

  private static Stream<Arguments> isSendingConfidential()
  {
    return Stream.of(
        arguments(ENCRYPTED_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),AFLEVEREN_ACTION),
        arguments(ENCRYPTED_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),BEVESTIG_AANLEVEREN_ACTION),
        arguments(ENCRYPTED_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),BEVESTIG_AFLEVEREN_ACTION),
        arguments(ENCRYPTED_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),AANLEVEREN_ACTION));
  }

  @ParameterizedTest
  @MethodSource
  void isSendingNotConfidential(String cpaId, List<PartyId> partyId, String role, String service, String action)
  {
    assertThat(cpaManager.isSendingConfidential(cpaId, partyId, role, service, action)).isFalse();
  }

  private static Stream<Arguments> isSendingNotConfidential()
  {
    return Stream.of(
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),AFLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),BEVESTIG_AANLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),BEVESTIG_AFLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),AANLEVEREN_ACTION),
        arguments(ENCRYPTED_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(DEFAULT_SERVICE),MESSAGE_ERROR_ACTION),
        arguments(ENCRYPTED_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(DEFAULT_SERVICE),ACKNOWLEDGMENT_ACTION));
  }

  @ParameterizedTest
  @MethodSource
  void getUri(String cpaId, List<PartyId> partyId, String role, String service, String action, String url)
  {
    assertThat(cpaManager.getReceivingUri(cpaId, partyId, role, service, action))
        .isEqualTo(url);
  }

  private static Stream<Arguments> getUri()
  {
    return Stream.of(
        arguments(DEFAULT_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),AFLEVEREN_ACTION,OVERHEID_URL),
        arguments(DEFAULT_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),BEVESTIG_AANLEVEREN_ACTION,OVERHEID_URL),
        arguments(DEFAULT_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(DEFAULT_SERVICE),ACKNOWLEDGMENT_ACTION,OVERHEID_URL),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),BEVESTIG_AFLEVEREN_ACTION,DIGIPOORT_URL),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),AANLEVEREN_ACTION,DIGIPOORT_URL),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(DEFAULT_SERVICE),ACKNOWLEDGMENT_ACTION,DIGIPOORT_URL));
  }

  @ParameterizedTest
  @MethodSource
  void getNoUri(String cpaId, List<PartyId> partyId, String role, String service, String action, String url)
  {
    assertThatIllegalStateException()
        .isThrownBy(() -> cpaManager.getReceivingUri(cpaId, partyId, role, service, action))
        .withMessageStartingWith("ReceiveDeliveryChannel");
  }

  private static Stream<Arguments> getNoUri()
  {
    return Stream.of(
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),AFLEVEREN_ACTION,DIGIPOORT_URL),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),BEVESTIG_AANLEVEREN_ACTION,DIGIPOORT_URL),
        arguments(DEFAULT_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),BEVESTIG_AFLEVEREN_ACTION,OVERHEID_URL),
        arguments(DEFAULT_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),AANLEVEREN_ACTION,OVERHEID_URL));
  }

  @ParameterizedTest
  @MethodSource
  void getSendSyncReply(String cpaId, List<PartyId> partyId, String role, String service, String action, SyncReplyModeType syncReplyMode)
  {
    assertThat(cpaManager.getSendSyncReply(cpaId, partyId, role, service, action))
        .hasValueSatisfying(mode -> assertThat(mode).isEqualTo(syncReplyMode));
  }

  private static Stream<Arguments> getSendSyncReply()
  {
    return Stream.of(
        arguments(SYNC_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),AFLEVEREN_ACTION,SyncReplyModeType.SIGNALS_AND_RESPONSE),
        arguments(SYNC_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),BEVESTIG_AANLEVEREN_ACTION,SyncReplyModeType.SIGNALS_AND_RESPONSE),
        arguments(SYNC_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(DEFAULT_SERVICE),ACKNOWLEDGMENT_ACTION,SyncReplyModeType.SIGNALS_AND_RESPONSE),
        arguments(SYNC_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),BEVESTIG_AFLEVEREN_ACTION,SyncReplyModeType.SIGNALS_AND_RESPONSE),
        arguments(SYNC_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),AANLEVEREN_ACTION,SyncReplyModeType.SIGNALS_AND_RESPONSE),
        arguments(SYNC_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(DEFAULT_SERVICE),ACKNOWLEDGMENT_ACTION,SyncReplyModeType.SIGNALS_AND_RESPONSE),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),AFLEVEREN_ACTION,SyncReplyModeType.NONE),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),BEVESTIG_AANLEVEREN_ACTION,SyncReplyModeType.NONE),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(DEFAULT_SERVICE),ACKNOWLEDGMENT_ACTION,SyncReplyModeType.NONE),
        arguments(DEFAULT_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),BEVESTIG_AFLEVEREN_ACTION,SyncReplyModeType.NONE),
        arguments(DEFAULT_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),AANLEVEREN_ACTION,SyncReplyModeType.NONE),
        arguments(DEFAULT_CPA_ID,asList(OVERHEID_PARTY_ID),OVERHEID_ROLE,CPAUtils.toString(DEFAULT_SERVICE),ACKNOWLEDGMENT_ACTION,SyncReplyModeType.NONE));
  }

  @ParameterizedTest
  @MethodSource
  void getNoSendSyncReply(String cpaId, List<PartyId> partyId, String role, String service, String action)
  {
    assertThat(cpaManager.getSendSyncReply(cpaId, partyId, role, service, action)).isEmpty();
  }

  private static Stream<Arguments> getNoSendSyncReply()
  {
    return Stream.of(
        arguments(DEFAULT_CPA_ID,emptyList(),DIGIPOORT_ROLE,CPAUtils.toString(AFLEVEREN_SERVICE),AFLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),null,CPAUtils.toString(AANLEVEREN_SERVICE),BEVESTIG_AANLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,null,BEVESTIG_AANLEVEREN_ACTION),
        arguments(DEFAULT_CPA_ID,asList(DIGIPOORT_PARTY_ID),DIGIPOORT_ROLE,CPAUtils.toString(AANLEVEREN_SERVICE),null));
  }

}
