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
package nl.clockwork.ebms.server.embedded.utils;

import java.util.List;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationRole;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PartyId;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ServiceType;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CPAUtils
{
	public static String toString(PartyId partyId)
	{
		return (partyId.getType() == null ? "" : partyId.getType() + ":") + partyId.getValue();
	}

	public static List<String> getPartyIds(CollaborationProtocolAgreement cpa)
	{
		return cpa.getPartyInfo().stream().map(p -> toString(p.getPartyId().get(0))).toList();
	}

	public static List<String> getPartyIdsByRoleName(CollaborationProtocolAgreement cpa, String roleName)
	{
		return cpa.getPartyInfo()
				.stream()
				.flatMap(
						p -> p.getCollaborationRole()
								.stream()
								.filter(r -> roleName == null || roleName.equals(r.getRole().getName()))
								.map(r -> toString(p.getPartyId().get(0))))
				.distinct()
				.toList();
	}

	public static List<String> getOtherPartyIds(CollaborationProtocolAgreement cpa, String partyId)
	{
		return cpa.getPartyInfo().stream().filter(p -> !partyId.equals(toString(p.getPartyId().get(0)))).map(p -> toString(p.getPartyId().get(0))).toList();
	}

	public static List<String> getOtherRoleNamesByPartyId(CollaborationProtocolAgreement cpa, String partyId)
	{
		return cpa.getPartyInfo()
				.stream()
				.filter(p -> !partyId.equals(toString(p.getPartyId().get(0))))
				.flatMap(p -> p.getCollaborationRole().stream().map(r -> r.getRole().getName()))
				.distinct()
				.toList();
	}

	public static List<String> getRoleNames(CollaborationProtocolAgreement cpa)
	{
		return cpa.getPartyInfo().stream().flatMap(p -> p.getCollaborationRole().stream().map(r -> r.getRole().getName())).distinct().toList();
	}

	public static List<String> getRoleNames(CollaborationProtocolAgreement cpa, String partyId)
	{
		return cpa.getPartyInfo()
				.stream()
				.filter(p -> partyId == null || partyId.equals(toString(p.getPartyId().get(0))))
				.flatMap(p -> p.getCollaborationRole().stream().map(r -> r.getRole().getName()))
				.distinct()
				.toList();
	}

	public static List<String> getOtherRoleNames(CollaborationProtocolAgreement cpa, String partyId, String roleName)
	{
		return cpa.getPartyInfo()
				.stream()
				.filter(p -> partyId == null || !partyId.equals(toString(p.getPartyId().get(0))))
				.flatMap(
						p -> p.getCollaborationRole().stream().filter(r -> roleName == null || !roleName.equals(r.getRole().getName())).map(r -> r.getRole().getName()))
				.distinct()
				.toList();
	}

	public static List<String> getServiceNames(CollaborationProtocolAgreement cpa, String roleName)
	{
		// return findRoles(cpa,roleName).map(r -> getServiceName(r.getServiceBinding().getService())).collect(Collectors.toList());
		return cpa.getPartyInfo()
				.stream()
				.flatMap(
						p -> p.getCollaborationRole()
								.stream()
								.filter(r -> r.getRole().getName().equals(roleName))
								.map(r -> getServiceName(r.getServiceBinding().getService())))
				.toList();
	}

	public static List<String> getServiceNamesCanSend(CollaborationProtocolAgreement cpa, String partyId, String roleName)
	{
		// return findRoles(cpa,roleName).filter(r -> r.getServiceBinding().getCanSend().size() > 0).map(r ->
		// getServiceName(r.getServiceBinding().getService()).collect(Collectors.toList());
		return cpa.getPartyInfo()
				.stream()
				.filter(p -> partyId == null || partyId.equals(toString(p.getPartyId().get(0))))
				.flatMap(
						p -> p.getCollaborationRole()
								.stream()
								.filter(r -> r.getRole().getName().equals(roleName) && !r.getServiceBinding().getCanSend().isEmpty())
								.map(r -> getServiceName(r.getServiceBinding().getService())))
				.toList();
	}

	public static List<String> getServiceNamesCanReceive(CollaborationProtocolAgreement cpa, String partyId, String roleName)
	{
		// return findRoles(cpa,partyId,roleName).filter(r -> r.getServiceBinding().getCanReceive().size() > 0).map(r ->
		// getServiceName(r.getServiceBinding().getService())).collect(Collectors.toList());
		return cpa.getPartyInfo()
				.stream()
				.filter(p -> partyId == null || partyId.equals(toString(p.getPartyId().get(0))))
				.flatMap(
						p -> p.getCollaborationRole()
								.stream()
								.filter(r -> r.getRole().getName().equals(roleName) && !r.getServiceBinding().getCanReceive().isEmpty())
								.map(r -> getServiceName(r.getServiceBinding().getService())))
				.toList();
	}

	public static List<String> getFromActionNamesCanSend(CollaborationProtocolAgreement cpa, String partyId, String roleName, String serviceName)
	{
		// return findRolesByService(cpa,partyId,roleName,serviceName).flatMap(r -> r.getServiceBinding().getCanSend().stream().map(cs ->
		// cs.getThisPartyActionBinding().getAction())).collect(Collectors.toList());
		return cpa.getPartyInfo()
				.stream()
				.filter(p -> partyId == null || partyId.equals(toString(p.getPartyId().get(0))))
				.flatMap(
						p -> p.getCollaborationRole()
								.stream()
								.filter(r -> r.getRole().getName().equals(roleName) && getServiceName(r.getServiceBinding().getService()).equals(serviceName))
								.flatMap(r -> r.getServiceBinding().getCanSend().stream().map(cs -> cs.getThisPartyActionBinding().getAction())))
				.toList();
	}

	public static List<String> getFromActionNamesCanReceive(CollaborationProtocolAgreement cpa, String partyId, String roleName, String serviceName)
	{
		// return findRolesByService(cpa,partyId,roleName,serviceName).flatMap(r -> r.getServiceBinding().getCanReceive().stream().map(cr ->
		// cr.getThisPartyActionBinding().getAction())).collect(Collectors.toList());
		return cpa.getPartyInfo()
				.stream()
				.filter(p -> partyId == null || partyId.equals(toString(p.getPartyId().get(0))))
				.flatMap(
						p -> p.getCollaborationRole()
								.stream()
								.filter(r -> r.getRole().getName().equals(roleName) && getServiceName(r.getServiceBinding().getService()).equals(serviceName))
								.flatMap(r -> r.getServiceBinding().getCanReceive().stream().map(cr -> cr.getThisPartyActionBinding().getAction())))
				.toList();
	}

	public static List<String> getFromActionNames(CollaborationProtocolAgreement cpa, String roleName, String serviceName)
	{
		// return findRolesByService(cpa,roleName,serviceName).flatMap(r -> r.getServiceBinding().getCanSend().stream().map(cs
		// ->cs.getThisPartyActionBinding().getAction())).collect(Collectors.toList())
		return cpa.getPartyInfo()
				.stream()
				.flatMap(
						p -> p.getCollaborationRole()
								.stream()
								.filter(r -> r.getRole().getName().equals(roleName))
								.filter(r -> getServiceName(r.getServiceBinding().getService()).equals(serviceName))
								.flatMap(r -> r.getServiceBinding().getCanSend().stream().map(cs -> cs.getThisPartyActionBinding().getAction())))
				.toList();
	}

	public static List<String> getToActionNames(CollaborationProtocolAgreement cpa, String roleName, String serviceName)
	{
		// return findRolesByService(cpa,roleName,serviceName).flatMap(r -> r.getServiceBinding().getCanReceive().stream().map(cr ->
		// cr.getThisPartyActionBinding().getAction())).collect(Collectors.toList());
		return cpa.getPartyInfo()
				.stream()
				.flatMap(
						p -> p.getCollaborationRole()
								.stream()
								.filter(r -> r.getRole().getName().equals(roleName))
								.filter(r -> getServiceName(r.getServiceBinding().getService()).equals(serviceName))
								.flatMap(r -> r.getServiceBinding().getCanReceive().stream().map(cr -> cr.getThisPartyActionBinding().getAction())))
				.toList();
	}

	private static String getServiceName(ServiceType service)
	{
		return (service.getType() == null ? "" : service.getType() + ":") + service.getValue();
	}

	@SuppressWarnings("unused")
	private static boolean equals(ServiceType service, String serviceName)
	{
		return getServiceName(service).equals(serviceName);
	}

	@SuppressWarnings("unused")
	private static Stream<CollaborationRole> findRoles(CollaborationProtocolAgreement cpa, String roleName)
	{
		return cpa.getPartyInfo().stream().flatMap(p -> p.getCollaborationRole().stream().filter(r -> r.getRole().getName().equals(roleName)));
	}

	@SuppressWarnings("unused")
	private static Stream<CollaborationRole> findRoles(CollaborationProtocolAgreement cpa, String partyId, String roleName)
	{
		return cpa.getPartyInfo()
				.stream()
				.filter(p -> partyId == null || partyId.equals(toString(p.getPartyId().get(0))))
				.flatMap(p -> p.getCollaborationRole().stream().filter(r -> r.getRole().getName().equals(roleName)));
	}

	@SuppressWarnings("unused")
	private static Stream<CollaborationRole> findRolesByService(CollaborationProtocolAgreement cpa, String partyId, String roleName, String serviceName)
	{
		// return findRoles(cpa,partyId,roleName).filter(r -> getServiceName(r.getServiceBinding().getService()).equals(serviceName));
		return cpa.getPartyInfo()
				.stream()
				.filter(p -> partyId == null || partyId.equals(toString(p.getPartyId().get(0))))
				.flatMap(
						p -> p.getCollaborationRole()
								.stream()
								.filter(r -> r.getRole().getName().equals(roleName) && getServiceName(r.getServiceBinding().getService()).equals(serviceName)));
	}

	@SuppressWarnings("unused")
	private static Stream<CollaborationRole> findRolesByService(CollaborationProtocolAgreement cpa, String roleName, String serviceName)
	{
		// return findRoles(cpa,roleName).filter(r -> getServiceName(r.getServiceBinding().getService()).equals(serviceName));
		return cpa.getPartyInfo()
				.stream()
				.flatMap(
						p -> p.getCollaborationRole()
								.stream()
								.filter(r -> r.getRole().getName().equals(roleName))
								.filter(r -> getServiceName(r.getServiceBinding().getService()).equals(serviceName)));
	}

}
