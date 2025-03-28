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
package nl.clockwork.ebms.model;

import java.io.Serializable;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CanReceive;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ServiceType;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId;

@Builder
@Value
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class ToPartyInfo implements Serializable
{
	private static final long serialVersionUID = 1L;
	@NonNull
	List<PartyId> partyIds;
	@NonNull
	String role;
	@NonNull
	ServiceType service;
	@NonNull
	CanReceive canReceive;
}
