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
	private interface LogInvoker
	{
		void log(String message);
	}
	private class TraceLogInvoker implements LogInvoker
	{
		@Override
		public void log(String message)
		{
			logger.trace(message);
		}
	}
	private class DebugLogInvoker implements LogInvoker
	{
		@Override
		public void log(String message)
		{
			logger.debug(message);
		}
	}
	private class InfoLogInvoker implements LogInvoker
	{
		@Override
		public void log(String message)
		{
			logger.info(message);
		}
	}
	private class WarnLogInvoker implements LogInvoker
	{
		@Override
		public void log(String message)
		{
			logger.warn(message);
		}
	}
	private class ErrorLogInvoker implements LogInvoker
	{
		@Override
		public void log(String message)
		{
			logger.error(message);
		}
	}
	private class FatalLogInvoker implements LogInvoker
	{
		@Override
		public void log(String message)
		{
			logger.fatal(message);
		}
	}
	public static enum LogLevel
	{
		TRACE,DEBUG,INFO,WARN,ERROR,FATAL;
	}
  protected transient Log logger = LogFactory.getLog(getClass());
  private LogInvoker logInvoker = new InfoLogInvoker();
  private LogLevel logLevel = LogLevel.INFO;
	private String message;

	@Override
	public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
	{
		logInvoker.log(this.message);
		return message;
	}

	public void setLogLevel(LogLevel logLevel)
	{
		this.logLevel = logLevel;
		setLogInvoker();
	}
	
	public void setMessage(String message)
	{
		this.message = message;
	}

	private void setLogInvoker()
	{
		if (logLevel.equals(LogLevel.TRACE))
			logInvoker = new TraceLogInvoker();
		else if (logLevel.equals(LogLevel.DEBUG))
			logInvoker = new DebugLogInvoker();
		else if (logLevel.equals(LogLevel.INFO))
			logInvoker = new InfoLogInvoker();
		else if (logLevel.equals(LogLevel.WARN))
			logInvoker = new WarnLogInvoker();
		else if (logLevel.equals(LogLevel.ERROR))
			logInvoker = new ErrorLogInvoker();
		else if (logLevel.equals(LogLevel.FATAL))
			logInvoker = new FatalLogInvoker();
	}
}
