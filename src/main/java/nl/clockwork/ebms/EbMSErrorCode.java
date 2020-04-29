package nl.clockwork.ebms;

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
	
	private final String errorCode;
	
	EbMSErrorCode(String errorCode)
	{
		this.errorCode = errorCode;
	}
	
	public final String errorCode()
	{
		return errorCode;
	}
	
}