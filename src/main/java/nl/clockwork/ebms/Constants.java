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
package nl.clockwork.ebms;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageStatusType;

public class Constants
{
	public static String MESSAGE = "MESSAGE";
	public static String EVENT_RECEIVED = "EVENT.RECEIVED";
	public static String EVENT_ACKNOWLEDGED = "EVENT.ACKNOWLEDGED";
	public static String EVENT_FAILED = "EVENT.FAILED";
	public static String EVENT_EXPIRED = "EVENT.EXPIRED";

	public static enum EbMSMessageStatus
	{
		UNAUTHORIZED(0,MessageStatusType.UN_AUTHORIZED), NOT_RECOGNIZED(1,MessageStatusType.NOT_RECOGNIZED), RECEIVED(2,MessageStatusType.RECEIVED), PROCESSED(3,MessageStatusType.PROCESSED), FORWARDED(4,MessageStatusType.FORWARDED), FAILED(5,MessageStatusType.RECEIVED), /*WAITING(6,MessageStatusType.RECEIVED), */SENDING(10), DELIVERY_FAILED(11), DELIVERED(12), EXPIRED(13);

		private final static Collection<EbMSMessageStatus> receiveStatus = Collections.unmodifiableCollection(Arrays.asList(UNAUTHORIZED,NOT_RECOGNIZED,RECEIVED,PROCESSED,FORWARDED,FAILED));
		private final static Collection<EbMSMessageStatus> sendStatus = Collections.unmodifiableCollection(Arrays.asList(SENDING,DELIVERY_FAILED,DELIVERED,EXPIRED));
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

		public final static EbMSMessageStatus get(String name)
		{
			for (EbMSMessageStatus status : EbMSMessageStatus.values())
				if (status.name().equals(name))
					return status;
			return null;
		}

		public final static EbMSMessageStatus get(MessageStatusType statusCode)
		{
			for (EbMSMessageStatus status : EbMSMessageStatus.values())
				if (status.statusCode().equals(statusCode))
					return status;
			return null;
		}

		public final static Collection<EbMSMessageStatus> getReceiveStatus()
		{
			return receiveStatus;
		}

		public final static Collection<EbMSMessageStatus> getSendStatus()
		{
			return sendStatus;
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
		SUCCEEDED(1), FAILED(2), EXPIRED(3);

		private final int id;

		EbMSEventStatus(int id) { this.id = id; }

		public final int id() { return id; }

		public final static EbMSEventStatus get(int id)
		{
			for (EbMSEventStatus status : EbMSEventStatus.values())
				if (status.id() == id)
					return status;
			return null;
		}
  }

	public static final String EBMS_SOAP_ACTION = "\"ebXML\"";
	public static final String EBMS_VERSION = "2.0";
	public static final String EBMS_SERVICE_URI = "urn:oasis:names:tc:ebxml-msg:service";
	public static final String EBMS_DEFAULT_LANGUAGE = "en-US";

	public static final String NSURI_SOAP_ENVELOPE = "http://schemas.xmlsoap.org/soap/envelope/";
	public static final String NSURI_SOAP_NEXT_ACTOR = "http://schemas.xmlsoap.org/soap/actor/next";
	public static final String CID = "cid:";

}
