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
package nl.clockwork.ebms.validation;

import javax.xml.bind.JAXBException;

import nl.clockwork.ebms.common.XMLMessageBuilder;

import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Error;

public class EbMSValidationException extends ValidationException
{
	private static final long serialVersionUID = 1L;
	private Error error;

	public EbMSValidationException(Error error)
	{
		this.error = error;
	}

	@Override
	public String getMessage()
	{
		try
		{
			return XMLMessageBuilder.getInstance(Error.class).handle(error);
		}
		catch (JAXBException e)
		{
			return error.getErrorCode();
		}
	}

	public Error getError()
	{
		return error;
	}
	
}
