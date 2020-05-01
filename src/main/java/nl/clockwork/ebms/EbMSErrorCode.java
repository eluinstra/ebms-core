package nl.clockwork.ebms;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
@Getter
public enum EbMSErrorCode
{
	VALUE_NOT_RECOGNIZED("ValueNotRecognized"),
	NOT_SUPPORTED("NotSupported"),
	INCONSISTENT("Inconsistent"),
	OTHER_XML("OtherXml"),
	DELIVERY_FAILURE("DeliveryFailure"),
	TIME_TO_LIVE_EXPIRED("TimeToLiveExpired"),
	SECURITY_FAILURE("SecurityFailure"),
	MIME_PROBLEM("MimeProblem"),
	UNKNOWN("Unknown");
	
	String errorCode;
}