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
  public enum EbMSMessageType
  {
  	IN(0), OUT(1);
		
		private final int id;
		
		EbMSMessageType(int id) { this.id = id; }
		
		public final int id() { return id; }
		
		public final static EbMSMessageType get(int id)
		{
			switch (id)
			{
				case 0:
					return EbMSMessageType.IN;
				case 1:
					return EbMSMessageType.OUT;
				default:
					return null;
			}
		}
  };

  public enum EbMSAcknowledgmentType
  {
  	ACKNOWLEDGMENT(0), MESSAGE_ERROR(1);
		
		private final int id;
		
		EbMSAcknowledgmentType(int id) { this.id = id; }
		
		public final int id() { return id; }
		
		public final static EbMSAcknowledgmentType get(int id)
		{
			switch (id)
			{
				case 0:
					return EbMSAcknowledgmentType.ACKNOWLEDGMENT;
				case 1:
					return EbMSAcknowledgmentType.MESSAGE_ERROR;
				default:
					return null;
			}
		}
  };

  public enum EbMSMessageStatus
  {
		RECEIVED(0), STORED(1), DELIVERED(2), DELIVERY_FAILED(3), ACKNOWLEDGED(4), ACKNOWLEDGMENT_FAILED(5), MESSAGE_ERROR_RAISED(6), MESSAGE_ERROR_FAILED(7);
		
		private final int id;
		
		EbMSMessageStatus(int id) { this.id = id; }
		
		public final int id() { return id; }
		
		public final static EbMSMessageStatus get(int id)
		{
			switch (id)
			{
				case 0:
					return EbMSMessageStatus.RECEIVED;
				case 1:
					return EbMSMessageStatus.STORED;
				case 2:
					return EbMSMessageStatus.DELIVERED;
				case 3:
					return EbMSMessageStatus.DELIVERY_FAILED;
				case 4:
					return EbMSMessageStatus.ACKNOWLEDGED;
				case 5:
					return EbMSMessageStatus.ACKNOWLEDGMENT_FAILED;
				case 6:
					return EbMSMessageStatus.MESSAGE_ERROR_RAISED;
				case 7:
					return EbMSMessageStatus.MESSAGE_ERROR_FAILED;
				default:
					return null;
			}
		}
  };

  public enum EbMSErrorCode
  {
  	VALUE_NOT_RECOGNIZED("ValueNotRecognized"), NOT_SUPPORTED("NotSupported"), INCONSISTENT("Inconsistent"), OTHER_XML("OtherXml"), DELIVERY_FAILURE("DeliveryFailure"), TIME_TO_LIVE_EXPIRED("TimeToLiveExpired"), SECURITY_FAILURE("SecurityFailure"), MIME_PROBLEM("MimeProblem"), UNKNOWN("Unknown");
		
		private final String errorCode;
		
		EbMSErrorCode(String errorCode) { this.errorCode = errorCode; }
		
		public final String errorCode() { return errorCode; }
		
  };

  public static final String[] allowedCipherSuites = new String[]{"TLS_DHE_RSA_WITH_AES_128_CBC_SHA","TLS_RSA_WITH_AES_128_CBC_SHA"};
  
	public static final String DEFAULT_FILENAME = "filename";

	public static final String EBMS_VERSION = "2.0";
	public static final String EBMS_SERVICE = "urn:oasis:names:tc:ebxml-msg:service";
	public static final String EBMS_ACKNOWLEDGEMENT = "Acknowledgment";
	public static final String EBMS_MESSAGE_ERROR = "MessageError";
	public static final String EBMS_ERROR = "EBMS.EBMS_ERROR";
	public static final String EBMS_SIGNATURE = "EBMS.SIGNATURE";
	public static final String EBMS_ID = "EBMS.EBMS_ID";
	public static final String EBMS_DATE = "EBMS.EBMS_DATE";
	public static final String CPA_ID = "EBMS.CPA_ID";

	public static final String EBMS_MESSAGE_ID = "EBMS.EBMS_MESSAGE_ID";

}
