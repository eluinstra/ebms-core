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
import java.util.stream.Stream;

import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageStatusType;

public class Constants
{
	public enum EbMSMessageStatus
	{
		UNAUTHORIZED(0,MessageStatusType.UN_AUTHORIZED), NOT_RECOGNIZED(1,MessageStatusType.NOT_RECOGNIZED), RECEIVED(2,MessageStatusType.RECEIVED), PROCESSED(3,MessageStatusType.PROCESSED), FORWARDED(4,MessageStatusType.FORWARDED), FAILED(5,MessageStatusType.RECEIVED), /*WAITING(6,MessageStatusType.RECEIVED), */SENDING(10), DELIVERY_FAILED(11), DELIVERED(12), EXPIRED(13);

		private static final Collection<EbMSMessageStatus> RECEIVESTATUS = Collections.unmodifiableCollection(Arrays.asList(UNAUTHORIZED,NOT_RECOGNIZED,RECEIVED,PROCESSED,FORWARDED,FAILED));
		private static final Collection<EbMSMessageStatus> SENDSTATUS = Collections.unmodifiableCollection(Arrays.asList(SENDING,DELIVERY_FAILED,DELIVERED,EXPIRED));
		private final int id;
		private final MessageStatusType statusCode;

		EbMSMessageStatus(int id)
		{
			this.id = id;
			this.statusCode = null;
		}
		
		EbMSMessageStatus(int id, MessageStatusType statusCode)
		{
			this.id = id;
			this.statusCode = statusCode;
		}

		public final int id()
		{
			return id;
		}

		public final MessageStatusType statusCode()
		{
			return statusCode;
		}

		public static Stream<EbMSMessageStatus> stream()
		{
			return Stream.of(EbMSMessageStatus.values());
		}

		public static final EbMSMessageStatus get(int id)
		{
			return EbMSMessageStatus.stream().filter(s -> s.id() == id).findFirst().orElse(null);//orElseThrow(() -> new IllegalStateException("Unsupported id " + id));
		}

		public static final EbMSMessageStatus get(String name)
		{
			return EbMSMessageStatus.stream().filter(s -> s.name().equals(name)).findFirst().orElse(null);
		}

		public static final EbMSMessageStatus get(MessageStatusType statusCode)
		{
			return EbMSMessageStatus.stream().filter(s -> s.statusCode.equals(statusCode)).findFirst().orElse(null);
		}

		public static final Collection<EbMSMessageStatus> getReceiveStatus()
		{
			return RECEIVESTATUS;
		}

		public static final Collection<EbMSMessageStatus> getSendStatus()
		{
			return SENDSTATUS;
		}
	};

  public enum EbMSErrorCode
  {
  	VALUE_NOT_RECOGNIZED("ValueNotRecognized"), NOT_SUPPORTED("NotSupported"), INCONSISTENT("Inconsistent"), OTHER_XML("OtherXml"), DELIVERY_FAILURE("DeliveryFailure"), TIME_TO_LIVE_EXPIRED("TimeToLiveExpired"), SECURITY_FAILURE("SecurityFailure"), MIME_PROBLEM("MimeProblem"), UNKNOWN("Unknown");
		
		private final String errorCode;
		
		EbMSErrorCode(String errorCode)
		{
			this.errorCode = errorCode;
		}
		
		public final String errorCode()
		{
			return errorCode;
		}
		
  };

  public enum EbMSAction
  {
		MESSAGE_ERROR("MessageError"), ACKNOWLEDGMENT("Acknowledgment"), STATUS_REQUEST("StatusRequest"), STATUS_RESPONSE("StatusResponse"), PING("Ping"), PONG("Pong");

		private final String action;

		EbMSAction(String action)
		{
			this.action = action;
		}

		public final String action()
		{
			return action;
		}
		
		public static Stream<EbMSAction> stream()
		{
			return Stream.of(EbMSAction.values());
		}
		
		public static final EbMSAction get(String action)
		{
			return EbMSAction.stream().filter(a -> a.action.equals(action)).findFirst().orElse(null);
		}

  };

  public enum EbMSEventStatus
  {
		SUCCEEDED(1), FAILED(2), EXPIRED(3);

		private final int id;

		EbMSEventStatus(int id)
		{
			this.id = id;
		}

		public final int id()
		{
			return id;
		}

		public static Stream<EbMSEventStatus> stream()
		{
			return Stream.of(EbMSEventStatus.values());
		}

		public static final EbMSEventStatus get(int id)
		{
			return EbMSEventStatus.stream().filter(s -> s.id() == id).findFirst().orElse(null);
		}
  }

	public enum EbMSMessageEventType
	{
		RECEIVED,DELIVERED,FAILED,EXPIRED;
		
		public static Stream<EbMSMessageEventType> stream()
		{
			return Stream.of(EbMSMessageEventType.values());
		}
	}

	public static final String EBMS_SOAP_ACTION = "\"ebXML\"";
	public static final String EBMS_VERSION = "2.0";
	public static final String EBMS_SERVICE_URI = "urn:oasis:names:tc:ebxml-msg:service";
	public static final String EBMS_DEFAULT_LANGUAGE = "en-US";

	public static final String NSURI_SOAP_ENVELOPE = "http://schemas.xmlsoap.org/soap/envelope/";
	public static final String NSURI_SOAP_NEXT_ACTOR = "http://schemas.xmlsoap.org/soap/actor/next";
	public static final String CID = "cid:";
	
	/* http servicecodes */
	public static final int SC_OK = 200;
	public static final int SC_NOCONTENT = 204;
	public static final int SC_BAD_REQUEST = 400;
	public static final int SC_INTERNAL_SERVER_ERROR = 500;
	
	/* time based */
	public static final int MINUTE_IN_MILLIS = 60000;
	
	public static final String MESSAGE_LOG = "nl.clockwork.ebms.message";

}
