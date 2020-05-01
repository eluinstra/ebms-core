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
package nl.clockwork.ebms.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.Endpoint;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PartyId;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.Transport;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.TransportReceiver;

import lombok.val;
import nl.clockwork.ebms.cpa.CPAUtils;

public class CPAUtilsTest {
	
	@SuppressWarnings("serial")
	class MsgPartyId extends org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId
	{
		
	}

	@Test
	public void testEquals()
	{
		val cpaPartyIds = new ArrayList<PartyId>();
		val headerPartyIds = new ArrayList<org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId>();
		
		cpaPartyIds.add(new PartyId());
		cpaPartyIds.get(0).setType("TYPE1");
		cpaPartyIds.get(0).setValue("VALUE1");		
		headerPartyIds.add(new MsgPartyId());
		headerPartyIds.get(0).setType("TYPE1");
		headerPartyIds.get(0).setValue("VALUE1");
		assertTrue(CPAUtils.equals(cpaPartyIds, headerPartyIds));
		
		headerPartyIds.get(0).setValue("VALUE2");
		assertFalse(CPAUtils.equals(cpaPartyIds, headerPartyIds));
		
		headerPartyIds.add(new MsgPartyId());
		headerPartyIds.get(1).setType("TYPE3");
		headerPartyIds.get(1).setValue("VALUE3");
		assertFalse(CPAUtils.equals(cpaPartyIds, headerPartyIds));
		
		cpaPartyIds.add(new PartyId());
		cpaPartyIds.get(1).setType("TYPE3");
		cpaPartyIds.get(1).setValue("VALUE3");
		assertFalse(CPAUtils.equals(cpaPartyIds, headerPartyIds));
		
		headerPartyIds.get(0).setValue("VALUE1");
		assertTrue(CPAUtils.equals(cpaPartyIds, headerPartyIds));

		cpaPartyIds.get(0).setType(null);
		assertFalse(CPAUtils.equals(cpaPartyIds, headerPartyIds));
	}
	
	@Test
	public void getHostname()
	{
		assertEquals("", CPAUtils.getHostname(null));

		val dc = new DeliveryChannel();
		assertEquals("", CPAUtils.getHostname(dc));

		val ep = new Endpoint();
		dc.setTransportId(new Transport());
		((Transport) dc.getTransportId()).setTransportId("test");
		assertEquals("", CPAUtils.getHostname(dc));
		
		((Transport) dc.getTransportId()).setTransportReceiver(new TransportReceiver());
		((Transport) dc.getTransportId()).getTransportReceiver().getEndpoint().add(ep);
		ep.setUri("http://dummy.nl/test");
		assertEquals("dummy.nl", CPAUtils.getHostname(dc));
		
		ep.setUri("DUMMMY");
		assertEquals("", CPAUtils.getHostname(dc));
		
	}
	
}
