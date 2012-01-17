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
package nl.clockwork.mule.ebms;

public class Constants
{
  public static enum EbMSMessageStatus
  {
		UNAUTHORIZED(0,"UnAuthorized"), NOT_RECOGNIZED(1,"NotRecognized"), RECEIVED(2,"Received"), PROCESSED(3,"Processed"), FORWARDED(4,"Forwarded")/*, STORED(10), SENT(11), FAILED(12), ACKNOWLEDGED(13), EXPIRED(14)*/;

		private final int id;
		private final String statusCode;

		EbMSMessageStatus(int id) { this.id = id; this.statusCode = null; }
		EbMSMessageStatus(int id, String statusCode) { this.id = id; this.statusCode = statusCode; }

		public final int id() { return id; }

		public final String statusCode() { return statusCode; }

		public final static EbMSMessageStatus get(int id)
		{
			switch (id)
			{
				case 0:
					return EbMSMessageStatus.UNAUTHORIZED;
				case 1:
					return EbMSMessageStatus.NOT_RECOGNIZED;
				case 2:
					return EbMSMessageStatus.RECEIVED;
				case 3:
					return EbMSMessageStatus.PROCESSED;
				case 4:
					return EbMSMessageStatus.FORWARDED;
				default:
					return null;
			}
		}

		public final static EbMSMessageStatus get(String id)
		{
			if (EbMSMessageStatus.UNAUTHORIZED.statusCode.equals(id))
				return EbMSMessageStatus.UNAUTHORIZED;
			if (EbMSMessageStatus.NOT_RECOGNIZED.statusCode.equals(id))
				return EbMSMessageStatus.NOT_RECOGNIZED;
			if (EbMSMessageStatus.RECEIVED.statusCode.equals(id))
				return EbMSMessageStatus.RECEIVED;
			if (EbMSMessageStatus.PROCESSED.statusCode.equals(id))
				return EbMSMessageStatus.PROCESSED;
			if (EbMSMessageStatus.FORWARDED.statusCode.equals(id))
				return EbMSMessageStatus.FORWARDED;
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

  public static final String[] allowedCipherSuites = new String[]{"TLS_DHE_RSA_WITH_AES_128_CBC_SHA","TLS_RSA_WITH_AES_128_CBC_SHA"};
  
	public static final String DEFAULT_FILENAME = "filename";

	public static final String CPA_ID = "EBMS.CPA_ID";

	public static final String EBMS_VERSION = "2.0";
	public static final String EBMS_SERVICE = "urn:oasis:names:tc:ebxml-msg:service";
	public static final String EBMS_MESSAGE_ERROR = "MessageError";
	public static final String EBMS_ACKNOWLEDGEMENT = "Acknowledgment";
	public static final String EBMS_PING_MESSAGE = "Ping";
	public static final String EBMS_PONG_MESSAGE = "Pong";

	public static final String EBMS_ERROR = "EBMS.EBMS_ERROR";
	public static final String EBMS_SIGNATURE = "EBMS.SIGNATURE";

	public static final String EBMS_MESSAGE = "EBMS.EBMS_MESSAGE";
	public static final String EBMS_MESSAGE_ID = "EBMS.EBMS_MESSAGE_ID";
	public static final String EBMS_MESSAGE_STATUS = "EBMS.EBMS_MESSAGE_STATUS";
	public static final String EBMS_DELEGATE_PATH = "EBMS.EBMS_DELEGATE_PATH";

}
