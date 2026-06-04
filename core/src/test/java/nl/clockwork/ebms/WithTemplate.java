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
package nl.clockwork.ebms;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import lombok.val;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

public interface WithTemplate
{
	default TemplateEngine templateEngine()
	{
		val templateEngine = new TemplateEngine();
		templateEngine.setTemplateResolver(templateResolver());
		return templateEngine;
	}

	private ClassLoaderTemplateResolver templateResolver()
	{
		val result = new ClassLoaderTemplateResolver();
		result.setTemplateMode(TemplateMode.TEXT);
		result.setCharacterEncoding("UTF-8");
		result.setPrefix("/nl/clockwork/ebms/soap/");
		result.setSuffix(".xml");
		return result;
	}

	default String insertCpa(TemplateEngine templateEngine, Context context)
	{
		return templateEngine.process("InsertCPA", context);
	}

	default Context insertCpaContext(String cpa)
	{
		val result = new Context();
		result.setVariable("cpa", cpa);
		result.setVariable("overwrite", true);
		return result;
	}

	default String ebMSPing(TemplateEngine templateEngine, Context context)
	{
		return templateEngine.process("EbMSPing", context);
	}

	default Context ebMSPingContext(String uuid)
	{
		var result = new Context();
		result.setVariable("uuid", uuid);
		result.setVariable("timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date()));
		result.setVariable("timeToLive", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date(System.currentTimeMillis() + 3600 * 1000)));
		return result;
	}

	default String ebMSMessage(TemplateEngine templateEngine, Context context)
	{
		return templateEngine.process("EbMSMessage", context);
	}

	default Context ebMSMessageContext(String uuid)
	{
		var result = new Context();
		result.setVariable("uuid", uuid);
		result.setVariable("timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date()));
		result.setVariable("cpaId", "cpaStubEBF.rm.http.unsigned.sync");
		result.setVariable("partyType", "urn:osb:oin");
		result.setVariable("fromPartyId", "00000000000000000000");
		result.setVariable("fromRole", "DIGIPOORT");
		result.setVariable("toPartyId", "00000000000000000001");
		result.setVariable("toRole", "OVERHEID");
		result.setVariable("serviceType", "urn:osb:services");
		result.setVariable("service", "osb:afleveren:1.1$1.0");
		result.setVariable("action", "afleveren");
		result.setVariable("timeToLive", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date(System.currentTimeMillis() + 3600 * 1000)));
		return result;
	}

	default String ebMSMessageWithAttachments(TemplateEngine templateEngine, Context context)
	{
		return templateEngine.process("EbMSMessageWithAttachments", context);
	}

	default Context ebMSMessageContextWithAttachments(String uuid, List<String> cids)
	{
		var result = new Context();
		result.setVariable("uuid", uuid);
		result.setVariable("timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date()));
		result.setVariable("cpaId", "cpaStubEBF.rm.http.unsigned.sync");
		result.setVariable("partyType", "urn:osb:oin");
		result.setVariable("fromPartyId", "00000000000000000000");
		result.setVariable("fromRole", "DIGIPOORT");
		result.setVariable("toPartyId", "00000000000000000001");
		result.setVariable("toRole", "OVERHEID");
		result.setVariable("serviceType", "urn:osb:services");
		result.setVariable("service", "osb:afleveren:1.1$1.0");
		result.setVariable("action", "afleveren");
		result.setVariable("timeToLive", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date(System.currentTimeMillis() + 3600 * 1000)));
		result.setVariable("cids", cids);
		return result;
	}

	default String ebMSMessageStatus(TemplateEngine templateEngine, Context context)
	{
		return templateEngine.process("EbMSMessageStatus", context);
	}

	default Context ebMSMessageStatusContext(String uuid, String refToMessageId)
	{
		var result = new Context();
		result.setVariable("uuid", uuid);
		result.setVariable("timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date()));
		result.setVariable("cpaId", "cpaStubEBF.rm.http.unsigned.sync");
		result.setVariable("partyType", "urn:osb:oin");
		result.setVariable("fromPartyId", "00000000000000000000");
		result.setVariable("fromRole", "DIGIPOORT");
		result.setVariable("toPartyId", "00000000000000000001");
		result.setVariable("toRole", "OVERHEID");
		result.setVariable("timeToLive", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date(System.currentTimeMillis() + 3600 * 1000)));
		result.setVariable("refToMessageId", refToMessageId);
		return result;
	}

	default String getUnprocessedMessageIds(TemplateEngine templateEngine, Context context)
	{
		return templateEngine.process("GetUnprocessedMessageIds", context);
	}

	default Context getUnprocessedMessageIdsContext()
	{
		val result = new Context();
		result.setVariable("cpaId", "cpaStubEBF.rm.http.unsigned.sync");
		result.setVariable("partyType", "urn:osb:oin");
		result.setVariable("fromPartyId", "00000000000000000000");
		result.setVariable("fromRole", "DIGIPOORT");
		result.setVariable("toPartyId", "00000000000000000001");
		result.setVariable("toRole", "OVERHEID");
		result.setVariable("serviceType", "urn:osb:services");
		result.setVariable("service", "osb:afleveren:1.1$1.0");
		result.setVariable("action", "afleveren");
		return result;
	}

	default String getMessage(TemplateEngine templateEngine, Context context)
	{
		return templateEngine.process("GetMessage", context);
	}

	default Context getMessageContext(String messageId)
	{
		val result = new Context();
		result.setVariable("messageId", messageId);
		return result;
	}

	default String processMessage(TemplateEngine templateEngine, Context context)
	{
		return templateEngine.process("ProcessMessage", context);
	}

	default Context processMessageContext(String messageId)
	{
		val result = new Context();
		result.setVariable("messageId", messageId);
		return result;
	}

	default String ebMSMessageInvalidMessageHeaderVersion(TemplateEngine templateEngine, Context context)
	{
		return templateEngine.process("EbMSMessageInvalidMessageHeaderVersion", context);
	}

	default Context ebMSMessageInvalidCPAIdContext(String uuid, String cpaId)
	{
		var result = new Context();
		result.setVariable("uuid", uuid);
		result.setVariable("timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date()));
		result.setVariable("cpaId", cpaId);
		result.setVariable("partyType", "urn:osb:oin");
		result.setVariable("fromPartyId", "00000000000000000000");
		result.setVariable("fromRole", "DIGIPOORT");
		result.setVariable("toPartyId", "00000000000000000001");
		result.setVariable("toRole", "OVERHEID");
		result.setVariable("serviceType", "urn:osb:services");
		result.setVariable("service", "osb:afleveren:1.1$1.0");
		result.setVariable("action", "afleveren");
		result.setVariable("timeToLive", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date(System.currentTimeMillis() + 3600 * 1000)));
		return result;
	}

	default Context ebMSMessageInvalidFromPartyIdContext(String uuid, String fromPartyId)
	{
		var result = new Context();
		result.setVariable("uuid", uuid);
		result.setVariable("timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date()));
		result.setVariable("cpaId", "cpaStubEBF.rm.http.unsigned.sync");
		result.setVariable("partyType", "urn:osb:oin");
		result.setVariable("fromPartyId", fromPartyId);
		result.setVariable("fromRole", "DIGIPOORT");
		result.setVariable("toPartyId", "00000000000000000001");
		result.setVariable("toRole", "OVERHEID");
		result.setVariable("serviceType", "urn:osb:services");
		result.setVariable("service", "osb:afleveren:1.1$1.0");
		result.setVariable("action", "afleveren");
		result.setVariable("timeToLive", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date(System.currentTimeMillis() + 3600 * 1000)));
		return result;
	}

	default Context ebMSMessageInvalidFromPartyTypeContext(String uuid, String partyType)
	{
		var result = new Context();
		result.setVariable("uuid", uuid);
		result.setVariable("timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date()));
		result.setVariable("cpaId", "cpaStubEBF.rm.http.unsigned.sync");
		result.setVariable("partyType", partyType);
		result.setVariable("fromPartyId", "00000000000000000000");
		result.setVariable("fromRole", "DIGIPOORT");
		result.setVariable("toPartyId", "00000000000000000001");
		result.setVariable("toRole", "OVERHEID");
		result.setVariable("serviceType", "urn:osb:services");
		result.setVariable("service", "osb:afleveren:1.1$1.0");
		result.setVariable("action", "afleveren");
		result.setVariable("timeToLive", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date(System.currentTimeMillis() + 3600 * 1000)));
		return result;
	}

	default String ebMSMessageMissingFromPartyId(TemplateEngine templateEngine, Context context)
	{
		return templateEngine.process("EbMSMessageMissingFromPartyId", context);
	}

	default Context ebMSMessageInvalidFromRoleContext(String uuid, String fromRole)
	{
		var result = new Context();
		result.setVariable("uuid", uuid);
		result.setVariable("timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date()));
		result.setVariable("cpaId", "cpaStubEBF.rm.http.unsigned.sync");
		result.setVariable("partyType", "urn:osb:oin");
		result.setVariable("fromPartyId", "00000000000000000000");
		result.setVariable("fromRole", fromRole);
		result.setVariable("toPartyId", "00000000000000000001");
		result.setVariable("toRole", "OVERHEID");
		result.setVariable("serviceType", "urn:osb:services");
		result.setVariable("service", "osb:afleveren:1.1$1.0");
		result.setVariable("action", "afleveren");
		result.setVariable("timeToLive", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date(System.currentTimeMillis() + 3600 * 1000)));
		return result;
	}

	default String ebMSMessageMissingFromRole(TemplateEngine templateEngine, Context context)
	{
		return templateEngine.process("EbMSMessageMissingFromRole", context);
	}

	default Context ebMSMessageInvalidToPartyIdContext(String uuid, String toPartyId)
	{
		var result = new Context();
		result.setVariable("uuid", uuid);
		result.setVariable("timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date()));
		result.setVariable("cpaId", "cpaStubEBF.rm.http.unsigned.sync");
		result.setVariable("partyType", "urn:osb:oin");
		result.setVariable("fromPartyId", "00000000000000000000");
		result.setVariable("fromRole", "DIGIPOORT");
		result.setVariable("toPartyId", toPartyId);
		result.setVariable("toRole", "OVERHEID");
		result.setVariable("serviceType", "urn:osb:services");
		result.setVariable("service", "osb:afleveren:1.1$1.0");
		result.setVariable("action", "afleveren");
		result.setVariable("timeToLive", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date(System.currentTimeMillis() + 3600 * 1000)));
		return result;
	}

	default Context ebMSMessageInvalidToPartyTypeContext(String uuid, String partyType)
	{
		var result = new Context();
		result.setVariable("uuid", uuid);
		result.setVariable("timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date()));
		result.setVariable("cpaId", "cpaStubEBF.rm.http.unsigned.sync");
		result.setVariable("partyType", partyType);
		result.setVariable("fromPartyId", "00000000000000000000");
		result.setVariable("fromRole", "DIGIPOORT");
		result.setVariable("toPartyId", "00000000000000000001");
		result.setVariable("toRole", "OVERHEID");
		result.setVariable("serviceType", "urn:osb:services");
		result.setVariable("service", "osb:afleveren:1.1$1.0");
		result.setVariable("action", "afleveren");
		result.setVariable("timeToLive", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date(System.currentTimeMillis() + 3600 * 1000)));
		return result;
	}

	default String ebMSMessageInvalidToPartyType(TemplateEngine templateEngine, Context context)
	{
		return templateEngine.process("EbMSMessageInvalidToPartyType", context);
	}

	default Context ebMSMessageInvalidToRoleContext(String uuid, String toRole)
	{
		var result = new Context();
		result.setVariable("uuid", uuid);
		result.setVariable("timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date()));
		result.setVariable("cpaId", "cpaStubEBF.rm.http.unsigned.sync");
		result.setVariable("partyType", "urn:osb:oin");
		result.setVariable("fromPartyId", "00000000000000000000");
		result.setVariable("fromRole", "DIGIPOORT");
		result.setVariable("toPartyId", "00000000000000000001");
		result.setVariable("toRole", toRole);
		result.setVariable("serviceType", "urn:osb:services");
		result.setVariable("service", "osb:afleveren:1.1$1.0");
		result.setVariable("action", "afleveren");
		result.setVariable("timeToLive", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date(System.currentTimeMillis() + 3600 * 1000)));
		return result;
	}

	default Context ebMSMessageInvalidServiceContext(String uuid, String service)
	{
		var result = new Context();
		result.setVariable("uuid", uuid);
		result.setVariable("timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date()));
		result.setVariable("cpaId", "cpaStubEBF.rm.http.unsigned.sync");
		result.setVariable("partyType", "urn:osb:oin");
		result.setVariable("fromPartyId", "00000000000000000000");
		result.setVariable("fromRole", "DIGIPOORT");
		result.setVariable("toPartyId", "00000000000000000001");
		result.setVariable("toRole", "OVERHEID");
		result.setVariable("serviceType", "urn:osb:services");
		result.setVariable("service", service);
		result.setVariable("action", "afleveren");
		result.setVariable("timeToLive", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date(System.currentTimeMillis() + 3600 * 1000)));
		return result;
	}

	default Context ebMSMessageInvalidServiceTypeContext(String uuid, String serviceType)
	{
		var result = new Context();
		result.setVariable("uuid", uuid);
		result.setVariable("timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date()));
		result.setVariable("cpaId", "cpaStubEBF.rm.http.unsigned.sync");
		result.setVariable("partyType", "urn:osb:oin");
		result.setVariable("fromPartyId", "00000000000000000000");
		result.setVariable("fromRole", "DIGIPOORT");
		result.setVariable("toPartyId", "00000000000000000001");
		result.setVariable("toRole", "OVERHEID");
		result.setVariable("serviceType", serviceType);
		result.setVariable("service", "osb:afleveren:1.1$1.0");
		result.setVariable("action", "afleveren");
		result.setVariable("timeToLive", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date(System.currentTimeMillis() + 3600 * 1000)));
		return result;
	}

	default Context ebMSMessageInvalidActionContext(String uuid, String action)
	{
		var result = new Context();
		result.setVariable("uuid", uuid);
		result.setVariable("timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date()));
		result.setVariable("cpaId", "cpaStubEBF.rm.http.unsigned.sync");
		result.setVariable("partyType", "urn:osb:oin");
		result.setVariable("fromPartyId", "00000000000000000000");
		result.setVariable("fromRole", "DIGIPOORT");
		result.setVariable("toPartyId", "00000000000000000001");
		result.setVariable("toRole", "OVERHEID");
		result.setVariable("serviceType", "urn:osb:services");
		result.setVariable("service", "osb:afleveren:1.1$1.0");
		result.setVariable("action", action);
		result.setVariable("timeToLive", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date(System.currentTimeMillis() + 3600 * 1000)));
		return result;
	}

	default String ebMSMessageInvalidRefToMessageId(TemplateEngine templateEngine, Context context)
	{
		return templateEngine.process("EbMSMessageInvalidRefToMessageId", context);
	}

	default Context ebMSMessageTimeToLiveExpiredContext(String uuid)
	{
		var result = new Context();
		result.setVariable("uuid", uuid);
		result.setVariable("timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date(System.currentTimeMillis() - 3600 * 1000)));
		result.setVariable("cpaId", "cpaStubEBF.rm.http.unsigned.sync");
		result.setVariable("partyType", "urn:osb:oin");
		result.setVariable("fromPartyId", "00000000000000000000");
		result.setVariable("fromRole", "DIGIPOORT");
		result.setVariable("toPartyId", "00000000000000000001");
		result.setVariable("toRole", "OVERHEID");
		result.setVariable("serviceType", "urn:osb:services");
		result.setVariable("service", "osb:afleveren:1.1$1.0");
		result.setVariable("action", "afleveren");
		result.setVariable("timeToLive", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date(System.currentTimeMillis() - 3600 * 1000)));
		return result;
	}

	default String ebMSMessageMissingDuplicateElimination(TemplateEngine templateEngine, Context context)
	{
		return templateEngine.process("EbMSMessageMissingDuplicateElimination", context);
	}

	default String ebMSMessageMissingAckRequested(TemplateEngine templateEngine, Context context)
	{
		return templateEngine.process("EbMSMessageMissingAckRequested", context);
	}

	default String ebMSMessageInvalidAckRequestedVersion(TemplateEngine templateEngine, Context context)
	{
		return templateEngine.process("EbMSMessageInvalidAckRequestedVersion", context);
	}

	default String ebMSMessageNextMshAckRequested(TemplateEngine templateEngine, Context context)
	{
		return templateEngine.process("EbMSMessageNextMshAckRequested", context);
	}

	default String ebMSMessageInvalidAckRequestedActor(TemplateEngine templateEngine, Context context)
	{
		return templateEngine.process("EbMSMessageInvalidAckRequestedActor", context);
	}

	default String ebMSMessageInvalidAckRequestedSigned(TemplateEngine templateEngine, Context context)
	{
		return templateEngine.process("EbMSMessageInvalidAckRequestedSigned", context);
	}

	default String ebMSMessageMissingSyncReply(TemplateEngine templateEngine, Context context)
	{
		return templateEngine.process("EbMSMessageMissingSyncReply", context);
	}

	default String ebMSMessageInvalidSyncReplyVersion(TemplateEngine templateEngine, Context context)
	{
		return templateEngine.process("EbMSMessageInvalidSyncReplyVersion", context);
	}

	default String ebMSMessageInvalidSyncReplyActor(TemplateEngine templateEngine, Context context)
	{
		return templateEngine.process("EbMSMessageInvalidSyncReplyActor", context);
	}

	default String ebMSMessageMessageOrder(TemplateEngine templateEngine, Context context)
	{
		return templateEngine.process("EbMSMessageMessageOrder", context);
	}

	default String ebMSMessageMissingManifest(TemplateEngine templateEngine, Context context)
	{
		return templateEngine.process("EbMSMessageMissingManifest", context);
	}

	default String ebMSMessageInvalidManifestVersion(TemplateEngine templateEngine, Context context)
	{
		return templateEngine.process("EbMSMessageInvalidManifestVersion", context);
	}

	default String ebMSMessageMissingReference(TemplateEngine templateEngine, Context context)
	{
		return templateEngine.process("EbMSMessageMissingReference", context);
	}

	default String ebMSPingXXE(TemplateEngine templateEngine, Context context)
	{
		return templateEngine.process("EbMSPingXXE", context);
	}

}
