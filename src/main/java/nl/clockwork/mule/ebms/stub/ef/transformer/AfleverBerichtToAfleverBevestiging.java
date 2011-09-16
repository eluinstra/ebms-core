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
package nl.clockwork.mule.ebms.stub.ef.transformer;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.datatype.DatatypeFactory;

import nl.clockwork.common.util.XMLMessageBuilder;
import nl.clockwork.mule.ebms.model.EbMSMessageContent;
import nl.clockwork.mule.ebms.stub.ef.model.afleveren.bevestiging.BevestigAfleverBericht;
import nl.clockwork.mule.ebms.stub.ef.model.afleveren.bevestiging.FoutType;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;

public class AfleverBerichtToAfleverBevestiging extends AbstractMessageAwareTransformer
{
	public AfleverBerichtToAfleverBevestiging()
	{
		registerSourceType(EbMSMessageContent.class);
	}
	
	@Override
	public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
	{
		try
		{
			EbMSMessageContent content = (EbMSMessageContent)message.getPayload();
			BevestigAfleverBericht afleverBevestiging = new BevestigAfleverBericht();
			FoutType error = (FoutType)message.getProperty("AFLEVERBERICHT_ERROR");
			if (error == null)
				afleverBevestiging.setTijdstempelOverheidsdeelnemer(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
			else
				afleverBevestiging.setFout(error);
			ByteArrayDataSource ds = new ByteArrayDataSource(XMLMessageBuilder.getInstance(BevestigAfleverBericht.class).handle(afleverBevestiging),"application/xml");
			ds.setName(name);
			List<DataSource> attachments = new ArrayList<DataSource>();
			attachments.add(ds);
			EbMSMessageContent result = new EbMSMessageContent(content.getConversationId(),attachments);
			return result;
		}
		catch (Exception e)
		{
			throw new TransformerException(this,e);
		}
	}

}
