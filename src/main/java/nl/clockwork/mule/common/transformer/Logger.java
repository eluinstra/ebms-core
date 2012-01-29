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
package nl.clockwork.mule.common.transformer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;

public class Logger extends AbstractMessageAwareTransformer
{
	public static enum LogLevel
	{
		TRACE,DEBUG,INFO,WARN,ERROR,FATAL;
	}
  protected transient Log logger = LogFactory.getLog(getClass());
  private LogLevel logLevel = LogLevel.INFO;
	private String message;

	@Override
	public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
	{
		if (logLevel.equals(LogLevel.TRACE))
			logger.trace(this.message);
		else if (logLevel.equals(LogLevel.DEBUG))
			logger.debug(this.message);
		else if (logLevel.equals(LogLevel.INFO))
			logger.info(this.message);
		else if (logLevel.equals(LogLevel.WARN))
			logger.warn(this.message);
		else if (logLevel.equals(LogLevel.ERROR))
			logger.error(this.message);
		else if (logLevel.equals(LogLevel.FATAL))
			logger.fatal(this.message);
		return message;
	}

	public void setLogLevel(LogLevel logLevel)
	{
		this.logLevel = logLevel;
	}
	
	public void setMessage(String message)
	{
		this.message = message;
	}

}
