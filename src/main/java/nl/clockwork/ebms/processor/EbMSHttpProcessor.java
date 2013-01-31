package nl.clockwork.ebms.processor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface EbMSHttpProcessor
{
	void process(HttpServletRequest request, HttpServletResponse response);
}
