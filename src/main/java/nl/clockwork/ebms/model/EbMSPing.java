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
package nl.clockwork.ebms.model;

import java.time.Instant;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.SyncReply;
import org.w3._2000._09.xmldsig.SignatureType;

import lombok.Builder;
import lombok.NonNull;
import nl.clockwork.ebms.EbMSAction;
import nl.clockwork.ebms.EbMSMessageFactory;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.validation.ValidatorException;

public class EbMSPing extends EbMSRequestMessage
{
	private static final long serialVersionUID = 1L;

	@Builder
	public EbMSPing(@NonNull MessageHeader messageHeader, SignatureType signature, SyncReply syncReply)
	{
		super(messageHeader,signature,syncReply);
	}

  public EbMSPong createPong(CPAManager cpaManager, Instant timestamp) throws ValidatorException, EbMSProcessorException
	{
		try
		{
			return EbMSPong.builder()
					.messageHeader(EbMSMessageFactory.createResponseMessageHeader(cpaManager,getMessageHeader(),timestamp,EbMSAction.PONG))
					.build();
		}
		catch (JAXBException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (DatatypeConfigurationException | TransformerFactoryConfigurationError e)
		{
			throw new EbMSProcessorException(e);
		}
	}

}
