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
import static nl.clockwork.ebms.cpa.CPATestUtils.DEFAULT_CPA_ID;
import static nl.clockwork.ebms.cpa.CPATestUtils.NOT_EXISTING_CPA_ID;
import static nl.clockwork.ebms.cpa.CPATestUtils.cpaCache;
import static nl.clockwork.ebms.cpa.CPATestUtils.loadDefaultCPA;
import static org.assertj.core.api.Assertions.assertThat;
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
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId;

import lombok.val;
import nl.clockwork.ebms.cpa.url.URLMapper;
import nl.clockwork.ebms.cpa.url.URLMappingDAO;
import nl.clockwork.ebms.model.EbMSPartyInfo;

@TestInstance(value = Lifecycle.PER_CLASS)
public class CPAManagerTest
{
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
    when(cpaDAO.getCPA(DEFAULT_CPA_ID)).thenReturn(loadDefaultCPA());
    when(urlMappingDAO.getURLMapping(anyString())).thenReturn(Optional.empty());
    urlMapper = new URLMapper(urlMappingDAO);
    cpaManager = new CPAManager(cpaDAO, urlMapper);
  }

  @Test
  void existsCPATest()
  {
    assertThat(cpaManager.existsCPA(DEFAULT_CPA_ID)).isTrue();
  }

  @Test
  void notExistsCPATest()
  {
    assertThat(cpaManager.existsCPA(NOT_EXISTING_CPA_ID)).isFalse();
  }

  @Test
  void getCPATest()
  {
    assertThat(cpaManager.getCPA(DEFAULT_CPA_ID).get().getCpaid()).isEqualTo(cpaCache.apply(DEFAULT_CPA_ID).get().getCpaid());
  }

  @ParameterizedTest
  @ValueSource(strings = {"2011-01-01T00:00:00Z","2020-01-01T00:00:00.00Z","2021-01-01T00:00:00Z"})
  void isValidTest(String timestamp)
  {
    assertThat(cpaManager.isValid(DEFAULT_CPA_ID,Instant.parse(timestamp))).isTrue();
  }

  @ParameterizedTest
  @MethodSource
  void isNotValidTest(String cpaId, String timestamp)
  {
    assertThat(cpaManager.isValid(cpaId,Instant.parse(timestamp))).isFalse();
  }

  private static Stream<Arguments> isNotValidTest()
  {
    return Stream.of(
      arguments(DEFAULT_CPA_ID,"2010-12-31T23:59:59Z"),
      arguments(DEFAULT_CPA_ID,"2021-01-01T00:00:01Z"),
      arguments(NOT_EXISTING_CPA_ID,"2020-01-01T00:00:00.00Z"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"urn:osb:oin:00000000000000000000","urn:osb:oin:00000000000000000001"})
  void existsPartyIdTest(String partyId)
  {
    assertThat(cpaManager.existsPartyId(DEFAULT_CPA_ID,partyId)).isTrue();
  }

  @ParameterizedTest
  @MethodSource
  void notExistsPartyIdTest(String cpaId, String partyId)
  {
    assertThat(cpaManager.existsPartyId(cpaId,partyId)).isFalse();
  }

  private static Stream<Arguments> notExistsPartyIdTest()
  {
    return Stream.of(
      arguments(DEFAULT_CPA_ID,null),
      arguments(DEFAULT_CPA_ID,""),
      arguments(DEFAULT_CPA_ID,"urn:osb:oin:00000000000000000002"),
      arguments(NOT_EXISTING_CPA_ID,""));
  }

  @ParameterizedTest
  @MethodSource
  void getEbMSPartyInfoTest(String cpaId, String partyId, EbMSPartyInfo expectedEbMSPartyInfo)
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

  private static Stream<Arguments> getEbMSPartyInfoTest()
  {
    return Stream.of(
      arguments(DEFAULT_CPA_ID,"urn:osb:oin:00000000000000000000",createEbMSPartyInfo(createPartyId("urn:osb:oin","00000000000000000000"),null)),
      arguments(DEFAULT_CPA_ID,"urn:osb:oin:00000000000000000001",createEbMSPartyInfo(createPartyId("urn:osb:oin","00000000000000000001"),null)));
  }

  private static EbMSPartyInfo createEbMSPartyInfo(PartyId partyId, String role) {
    return new EbMSPartyInfo(asList(partyId),role);
  }

  @ParameterizedTest
  @MethodSource
  void getNoEbMSPartyInfoTest(String cpaId, String partyId)
  {
    assertThat(cpaManager.getEbMSPartyInfo(cpaId,partyId)).isEmpty();
  }

  private static Stream<Arguments> getNoEbMSPartyInfoTest()
  {
    return Stream.of(
      arguments(DEFAULT_CPA_ID,null),
      arguments(DEFAULT_CPA_ID,""),
      arguments(DEFAULT_CPA_ID,"urn:osb:oin:00000000000000000002"),
      arguments(NOT_EXISTING_CPA_ID,""));
  }

  @ParameterizedTest
  @MethodSource
  void getPartyInfoTest(String cpaId, List<PartyId> partyIds, String expectedPartyName)
  {
    assertThat(cpaManager.getPartyInfo(cpaId,partyIds))
        .hasValueSatisfying(partyInfo -> assertThat(partyInfo.getPartyName()).isEqualTo(expectedPartyName));
  }

  private static Stream<Arguments> getPartyInfoTest()
  {
    return Stream.of(
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000000")),"Logius"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000001")),"Overheid"));
  }

  private static PartyId createPartyId(String type, String value) {
    val result = new PartyId();
    result.setType(type);
    result.setValue(value);
    return result;
  }

  @ParameterizedTest
  @MethodSource
  void getNoPartyInfoTest(String cpaId, List<PartyId> partyIds)
  {
    assertThat(cpaManager.getPartyInfo(cpaId,partyIds))
        .isEmpty();
  }

  private static Stream<Arguments> getNoPartyInfoTest()
  {
    return Stream.of(
      arguments(DEFAULT_CPA_ID,asList(createPartyId(null,"00000000000000000000"))),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin",null))),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("","00000000000000000000"))),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin",""))),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oinxxx","00000000000000000000"))),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000002"))),
      arguments(DEFAULT_CPA_ID,emptyList()),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000000"),createPartyId("urn:osb:oin","00000000000000000000"))),
      arguments(NOT_EXISTING_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000000"))));
  }

  @ParameterizedTest
  @MethodSource
  void canSendTest(String cpaId, List<PartyId> partyId, String role, String service, String action)
  {
    assertThat(cpaManager.canSend(cpaId,partyId,role,service,action)).isTrue();
  }

  private static Stream<Arguments> canSendTest()
  {
    return Stream.of(
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000000")),"DIGIPOORT","urn:osb:services:osb:afleveren:1.1$1.0","afleveren"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000000")),"DIGIPOORT","urn:osb:services:osb:aanleveren:1.1$1.0","bevestigAanleveren"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000001")),"OVERHEID","urn:osb:services:osb:afleveren:1.1$1.0","bevestigAfleveren"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000001")),"OVERHEID","urn:osb:services:osb:aanleveren:1.1$1.0","aanleveren"));
  }

  @ParameterizedTest
  @MethodSource
  void canNotSendTest(String cpaId, List<PartyId> partyId, String role, String service, String action)
  {
    assertThat(cpaManager.canSend(cpaId,partyId,role,service,action)).isFalse();
  }

  private static Stream<Arguments> canNotSendTest()
  {
    return Stream.of(
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000000")),"DIGIPOORT","urn:osb:services:osb:aanleveren:1.1$1.0","aanleveren"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000000")),"DIGIPOORT","urn:osb:services:osb:afleveren:1.1$1.0","bevestigAfleveren"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000001")),"OVERHEID","urn:osb:services:osb:aanleveren:1.1$1.0","bevestigAanleveren"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000001")),"OVERHEID","urn:osb:services:osb:afleveren:1.1$1.0","afleveren"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000000"),createPartyId("urn:osb:oin","00000000000000000000")),"DIGIPOORT","urn:osb:services:osb:afleveren:1.1$1.0","afleveren"),
      arguments(DEFAULT_CPA_ID,emptyList(),"DIGIPOORT","urn:osb:services:osb:afleveren:1.1$1.0","afleveren"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId(null,"00000000000000000000")),"DIGIPOORT","urn:osb:services:osb:afleveren:1.1$1.0","afleveren"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin",null)),"DIGIPOORT","urn:osb:services:osb:afleveren:1.1$1.0","afleveren"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000000")),null,"urn:osb:services:osb:afleveren:1.1$1.0","afleveren"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000000")),"DIGIPOORT",null,"afleveren"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000000")),"DIGIPOORT","urn:osb:services:osb:afleveren:1.1$1.0",null),
      arguments(NOT_EXISTING_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000000")),"DIGIPOORT","urn:osb:services:osb:afleveren:1.1$1.0","afleveren")
    );
  }

  @ParameterizedTest
  @MethodSource
  void canReceiveTest(String cpaId, List<PartyId> partyId, String role, String service, String action)
  {
    assertThat(cpaManager.canReceive(cpaId,partyId,role,service,action)).isTrue();
  }

  private static Stream<Arguments> canReceiveTest()
  {
    return Stream.of(
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000000")),"DIGIPOORT","urn:osb:services:osb:aanleveren:1.1$1.0","aanleveren"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000000")),"DIGIPOORT","urn:osb:services:osb:afleveren:1.1$1.0","bevestigAfleveren"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000001")),"OVERHEID","urn:osb:services:osb:aanleveren:1.1$1.0","bevestigAanleveren"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000001")),"OVERHEID","urn:osb:services:osb:afleveren:1.1$1.0","afleveren"));
  }

  @ParameterizedTest
  @MethodSource
  void canNotReceiveTest(String cpaId, List<PartyId> partyId, String role, String service, String action)
  {
    assertThat(cpaManager.canReceive(cpaId,partyId,role,service,action)).isFalse();
  }

  private static Stream<Arguments> canNotReceiveTest()
  {
    return Stream.of(
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000000")),"DIGIPOORT","urn:osb:services:osb:afleveren:1.1$1.0","afleveren"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000000")),"DIGIPOORT","urn:osb:services:osb:aanleveren:1.1$1.0","bevestigAanleveren"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000001")),"OVERHEID","urn:osb:services:osb:afleveren:1.1$1.0","bevestigAfleveren"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000001")),"OVERHEID","urn:osb:services:osb:aanleveren:1.1$1.0","aanleveren"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000000"),createPartyId("urn:osb:oin","00000000000000000000")),"DIGIPOORT","urn:osb:services:osb:afleveren:1.1$1.0","afleveren"),
      arguments(DEFAULT_CPA_ID,emptyList(),null,null,null),
      arguments(DEFAULT_CPA_ID,emptyList(),"DIGIPOORT","urn:osb:services:osb:aanleveren:1.1$1.0","aanleveren"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId(null,"00000000000000000000")),"DIGIPOORT","urn:osb:services:osb:aanleveren:1.1$1.0","aanleveren"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin",null)),"DIGIPOORT","urn:osb:services:osb:aanleveren:1.1$1.0","aanleveren"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000000")),null,"urn:osb:services:osb:aanleveren:1.1$1.0","aanleveren"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000000")),"DIGIPOORT",null,"aanleveren"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000000")),"DIGIPOORT","urn:osb:services:osb:aanleveren:1.1$1.0",null),
      arguments(NOT_EXISTING_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000000")),"DIGIPOORT","urn:osb:services:osb:aanleveren:1.1$1.0","aanleveren")
    );
  }

  @ParameterizedTest
  @MethodSource
  void getDeliveryChannelTest(String cpaId, String deliveryChannelId)
  {
    assertThat(cpaManager.getDeliveryChannel(cpaId,deliveryChannelId))
        .hasValueSatisfying(deliveryChannel -> assertThat(deliveryChannel.getChannelId()).isEqualTo(deliveryChannelId));
  }

  private static Stream<Arguments> getDeliveryChannelTest()
  {
    return Stream.of(
      arguments(DEFAULT_CPA_ID,"DIGIPOORT_defaultDeliveryChannel_ProfileBestEffortSigned"),
      arguments(DEFAULT_CPA_ID,"DIGIPOORT_defaultDeliveryChannel_ProfileReliableMessagingSigned"),
      arguments(DEFAULT_CPA_ID,"OVERHEID_defaultDeliveryChannel_ProfileBestEffortSigned"),
      arguments(DEFAULT_CPA_ID,"OVERHEID_defaultDeliveryChannel_ProfileReliableMessagingSigned"));
  }

  @ParameterizedTest
  @MethodSource
  void getNoDeliveryChannelTest(String cpaId, String deliveryChannelId)
  {
    assertThat(cpaManager.getDeliveryChannel(cpaId,deliveryChannelId)).isEmpty();
  }

  private static Stream<Arguments> getNoDeliveryChannelTest()
  {
    return Stream.of(
      arguments(DEFAULT_CPA_ID,null),
      arguments(DEFAULT_CPA_ID,""),
      arguments(DEFAULT_CPA_ID,"DIGIPOORT_defaultDeliveryChannel_ProfileBestEffortSignedxxx"));
  }

  @ParameterizedTest
  @MethodSource
  void getDefaultDeliveryChannelTest(String cpaId, List<PartyId> partyId, String action, String deliveryChannelId)
  {
    assertThat(cpaManager.getDefaultDeliveryChannel(cpaId,partyId,action))
        .hasValueSatisfying(deliveryChannel -> assertThat(deliveryChannel.getChannelId()).isEqualTo(deliveryChannelId));
  }

  private static Stream<Arguments> getDefaultDeliveryChannelTest()
  {
    return Stream.of(
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000000")),"afleveren","DIGIPOORT_defaultDeliveryChannel_ProfileBestEffortSigned"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000000")),"aanleveren","DIGIPOORT_defaultDeliveryChannel_ProfileBestEffortSigned"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000000")),"Acknowledgment","DIGIPOORT_defaultDeliveryChannel_ProfileBestEffortSigned"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000000")),"xxx","DIGIPOORT_defaultDeliveryChannel_ProfileBestEffortSigned"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000000")),null,"DIGIPOORT_defaultDeliveryChannel_ProfileBestEffortSigned"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000001")),"aanleveren","OVERHEID_defaultDeliveryChannel_ProfileBestEffortSigned"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000001")),"afleveren","OVERHEID_defaultDeliveryChannel_ProfileBestEffortSigned"));
  }

  @ParameterizedTest
  @MethodSource
  void getNoDefaultDeliveryChannelTest(String cpaId, List<PartyId> partyId, String action)
  {
    assertThat(cpaManager.getDefaultDeliveryChannel(cpaId,partyId,action)).isEmpty();
  }

  private static Stream<Arguments> getNoDefaultDeliveryChannelTest()
  {
    return Stream.of(
      arguments(DEFAULT_CPA_ID,emptyList(),"aanleveren"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin","00000000000000000000"),createPartyId("urn:osb:oin","00000000000000000000")),"aanleveren"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId(null,"00000000000000000000")),"aanleveren"),
      arguments(DEFAULT_CPA_ID,asList(createPartyId("urn:osb:oin",null)),"aanleveren"));
  }

}
