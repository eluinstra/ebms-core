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
package nl.clockwork.mule.ebms.stub.ebf.transformer;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.datatype.DatatypeFactory;

import nl.clockwork.ebms.common.XMLMessageBuilder;
import nl.clockwork.ebms.model.EbMSDataSource;
import nl.clockwork.ebms.model.EbMSMessageContent;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.logius.digipoort.ebms._2_0.afleverservice._1.AfleverBericht;
import nl.logius.digipoort.ebms._2_0.afleverservice._1.BevestigAfleverBericht;
import nl.logius.digipoort.ebms._2_0.afleverservice._1.FoutType;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;

public class AfleverBerichtToAfleverBevestiging extends AbstractMessageTransformer
{
	private String cpaId;
	private String service;
	private String action;

	public AfleverBerichtToAfleverBevestiging()
	{
		registerSourceType(DataTypeFactory.create(EbMSMessageContent.class));
	}
	
	@Override
	public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
	{
		try
		{
			EbMSMessageContent content = (EbMSMessageContent)message.getPayload();
			AfleverBericht afleverBericht = XMLMessageBuilder.getInstance(AfleverBericht.class).handle(new String(content.getDataSources().iterator().next().getContent()));
			BevestigAfleverBericht afleverBevestiging = new BevestigAfleverBericht();

			afleverBevestiging.setKenmerk(afleverBericht.getKenmerk());
			afleverBevestiging.setBerichtsoort(afleverBericht.getBerichtsoort());

			FoutType error = (FoutType)message.getProperty("AFLEVERBERICHT_ERROR",PropertyScope.SESSION);
			if (error == null)
				afleverBevestiging.setTijdstempelAfgeleverd(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
			else
				afleverBevestiging.setFout(error);

			List<EbMSDataSource> dataSources = new ArrayList<EbMSDataSource>();
			dataSources.add(new EbMSDataSource(null,"application/xml",XMLMessageBuilder.getInstance(BevestigAfleverBericht.class).handle(afleverBevestiging).getBytes()));

			return new EbMSMessageContent(new EbMSMessageContext(cpaId,service,action,content.getContext().getConversationId()),dataSources);
		}
		catch (Exception e)
		{
			throw new TransformerException(this,e);
		}
	}

	public void setCpaId(String cpaId)
	{
		this.cpaId = cpaId;
	}
	
	public void setService(String service)
	{
		this.service = service;
	}
	
	public void setAction(String action)
	{
		this.action = action;
	}
}
