package nl.clockwork.ebms.common.util;


public class HTTPUtils
{

	public static String getCharSet(String contentType)
	{
		String charset = null;
		for (String param: contentType.replace(" ","").split(";"))
		{
			if (param.startsWith("charset="))
			{
				charset = param.split("=",2)[1];
				break;
			}
		}
		return charset;
	}
	
}
