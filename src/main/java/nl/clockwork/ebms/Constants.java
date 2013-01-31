/*******************************************************************************
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
 ******************************************************************************/
package nl.clockwork.ebms;

import nl.clockwork.ebms.model.EbMSAction;
import nl.clockwork.ebms.model.EbMSService;
import nl.clockwork.ebms.model.ebxml.MessageStatusType;
import nl.clockwork.ebms.model.ebxml.Service;

public class Constants
{
  public static enum EbMSMessageStatus
  {
		UNAUTHORIZED(0,MessageStatusType.UN_AUTHORIZED), NOT_RECOGNIZED(1,MessageStatusType.NOT_RECOGNIZED), RECEIVED(2,MessageStatusType.RECEIVED), PROCESSED(3,MessageStatusType.PROCESSED), FORWARDED(4,MessageStatusType.FORWARDED), FAILED(5,MessageStatusType.RECEIVED), DELIVERED(10), DELIVERY_FAILED(11);

		private final int id;
		private final MessageStatusType statusCode;

		EbMSMessageStatus(int id) { this.id = id; this.statusCode = null; }
		EbMSMessageStatus(int id, MessageStatusType statusCode) { this.id = id; this.statusCode = statusCode; }

		public final int id() { return id; }

		public final MessageStatusType statusCode() { return statusCode; }

		public final static EbMSMessageStatus get(int id)
		{
			for (EbMSMessageStatus status : EbMSMessageStatus.values())
				if (status.id() == id)
					return status;
			return null;
		}

		public final static EbMSMessageStatus get(String id)
		{
			for (EbMSMessageStatus status : EbMSMessageStatus.values())
				if (status.name().equals(id))
					return status;
			return null;
		}
  };

  public enum EbMSErrorCode
  {
  	VALUE_NOT_RECOGNIZED("ValueNotRecognized"), NOT_SUPPORTED("NotSupported"), INCONSISTENT("Inconsistent"), OTHER_XML("OtherXml"), DELIVERY_FAILURE("DeliveryFailure"), TIME_TO_LIVE_EXPIRED("TimeToLiveExpired"), SECURITY_FAILURE("SecurityFailure"), MIME_PROBLEM("MimeProblem"), UNKNOWN("Unknown");
		
		private final String errorCode;
		
		EbMSErrorCode(String errorCode) { this.errorCode = errorCode; }
		
		public final String errorCode() { return errorCode; }
		
  };

  public enum EbMSErrorLocation
  {
  	UNKNOWN("Unknown");

		private final String location;
		
		EbMSErrorLocation(String location) { this.location = location; }
		
		public final String location() { return location; }
		
  }

  public static enum EbMSMessageType
  {
		MESSAGE(0), MESSAGE_ERROR(1,new EbMSAction(EBMS_SERVICE_MESSAGE,"MessageError")), ACKNOWLEDGMENT(2,new EbMSAction(EBMS_SERVICE_MESSAGE,"Acknowledgment")), STATUS_REQUEST(3,new EbMSAction(EBMS_SERVICE_MESSAGE,"StatusRequest")), STATUS_RESPONSE(4,new EbMSAction(EBMS_SERVICE_MESSAGE,"StatusResponse")), PING(5,new EbMSAction(EBMS_SERVICE_MESSAGE,"Ping")), PONG(6,new EbMSAction(EBMS_SERVICE_MESSAGE,"Pong")), SERVICE_MESSAGE(7,new EbMSAction(EBMS_SERVICE_MESSAGE,null));

		private final int id;
		private final EbMSAction action;

		EbMSMessageType(int id) { this.id = id; this.action = null; }
		EbMSMessageType(int id, EbMSAction action) { this.id = id; this.action = action; }

		public final int id() { return id; }

		public final EbMSAction action() { return action; }

		public final static EbMSMessageType get(int id)
		{
			for (EbMSMessageType type : EbMSMessageType.values())
				if (type.id() == id)
					return type;
			return null;
		}
  };

	public static final String DEFAULT_FILENAME = "file";

	public static final String EBMS_VERSION = "2.0";
	public static final String EBMS_SERVICE_URI = "urn:oasis:names:tc:ebxml-msg:service";
	public static final Service EBMS_SERVICE_MESSAGE = new EbMSService(EBMS_SERVICE_URI);
	public static final String EBMS_ERROR_CODE_CONTEXT = EBMS_SERVICE_URI + ":errors";
	public static final String EBMS_DEFAULT_LANGUAGE = "en-US";

	public static final String NAMESPACE_URI_XML_NS = "http://www.w3.org/2000/xmlns/";
	public static final String NAMESPACE_URI_SOAP_ENVELOPE = "http://schemas.xmlsoap.org/soap/envelope/";
	public static final String TRANSFORM_ALGORITHM_XPATH = "http://www.w3.org/TR/1999/REC-xpath-19991116";
	public static final String NAMESPACE_PREFIX_SOAP_ENVELOPE = "soap";
	//public static final String TRANSFORM_XPATH = "not(ancestor-or-self::node()[@" + NAMESPACE_PREFIX_SOAP_ENVELOPE + ":actor=\"" + ACTOR_NEXT_MSH_URN + "\"] | ancestor-or-self::node()[@" + NAMESPACE_PREFIX_SOAP_ENVELOPE + ":actor=\"" + ACTOR_NEXT_MSH_SCHEMAS + "\"])";
	public static final String TRANSFORM_XPATH = "not(ancestor-or-self::node()[@" + NAMESPACE_PREFIX_SOAP_ENVELOPE + ":actor=\"urn:oasis:names:tc:ebxml-msg:service:nextMSH\"]|ancestor-or-self::node()[@" + NAMESPACE_PREFIX_SOAP_ENVELOPE + ":actor=\"http://schemas.xmlsoap.org/soap/actor/next\"])";
	public static final String NAMESPACE_PREFIX_DS = "ds";
	public static final String CID = "cid:";

}
