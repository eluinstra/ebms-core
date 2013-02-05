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

import nl.clockwork.ebms.model.ebxml.MessageStatusType;

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

  public static enum EbMSAction
  {
		MESSAGE_ERROR("MessageError"), ACKNOWLEDGMENT("Acknowledgment"), STATUS_REQUEST("StatusRequest"), STATUS_RESPONSE("StatusResponse"), PING("Ping"), PONG("Pong");

		private final String action;

		EbMSAction(String action) { this.action = action; }

		public final String action() { return action; }

  };

  public static enum EbMSEventStatus
  {
		UNPROCESSED(0), PROCESSED(1), FAILED(2);

		private final int id;

		EbMSEventStatus(int id) { this.id = id; }

		public final int id() { return id; }
 }

	public static final String DEFAULT_FILENAME = "file";

	public static final String EBMS_SOAP_ACTION = "\"ebXML\"";
	public static final String EBMS_VERSION = "2.0";
	public static final String EBMS_SERVICE_URI = "urn:oasis:names:tc:ebxml-msg:service";
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
