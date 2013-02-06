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
package nl.clockwork.mule.ebms.stub.ebf.transformer;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.datatype.DatatypeFactory;

import nl.clockwork.ebms.common.util.XMLMessageBuilder;
import nl.clockwork.ebms.model.EbMSDataSource;
import nl.clockwork.ebms.model.EbMSMessageContent;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.mule.ebms.stub.ebf.model.aanleveren.bericht.AanleverBericht;
import nl.clockwork.mule.ebms.stub.ebf.model.aanleveren.bevestiging.BevestigAanleverBericht;
import nl.clockwork.mule.ebms.stub.ebf.model.aanleveren.bevestiging.FoutType;
import nl.clockwork.mule.ebms.stub.ebf.model.aanleveren.bevestiging.IdentiteitType;

import org.apache.commons.lang.StringUtils;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;

public class AanleverBerichtToAanleverBevestiging extends AbstractMessageTransformer
{
	private String cpaId;
	private String service;
	private String action;

	public AanleverBerichtToAanleverBevestiging()
	{
		registerSourceType(DataTypeFactory.create(EbMSMessageContent.class));
	}
	
	@Override
	public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
	{
		try
		{
			EbMSMessageContent content = (EbMSMessageContent)message.getPayload();
			AanleverBericht aanleverBericht = XMLMessageBuilder.getInstance(AanleverBericht.class).handle(new String(content.getDataSources().iterator().next().getContent()));
			BevestigAanleverBericht aanleverBevestiging = new BevestigAanleverBericht();


			aanleverBevestiging.setKenmerk(StringUtils.isAlpha(aanleverBericht.getKenmerk()) ? aanleverBericht.getKenmerk() : "kenmerk");
			aanleverBevestiging.setBerichtsoort(aanleverBericht.getBerichtsoort());
			aanleverBevestiging.setAanleverkenmerk(aanleverBericht.getAanleverkenmerk());
			aanleverBevestiging.setEerderAanleverkenmerk(aanleverBericht.getEerderAanleverkenmerk());
			aanleverBevestiging.setIdentiteitBelanghebbende(new IdentiteitType());
			aanleverBevestiging.getIdentiteitBelanghebbende().setNummer(aanleverBericht.getIdentiteitBelanghebbende().getNummer());
			aanleverBevestiging.getIdentiteitBelanghebbende().setType(aanleverBericht.getIdentiteitBelanghebbende().getType());
			aanleverBevestiging.setRolBelanghebbende(aanleverBericht.getRolBelanghebbende());
			if (aanleverBericht.getIdentiteitOntvanger() != null)
			{
				aanleverBevestiging.setIdentiteitOntvanger(new IdentiteitType());
				aanleverBevestiging.getIdentiteitOntvanger().setNummer(aanleverBericht.getIdentiteitOntvanger().getNummer());
				aanleverBevestiging.getIdentiteitOntvanger().setType(aanleverBericht.getIdentiteitOntvanger().getType());
			}
			else
			{
				aanleverBevestiging.setIdentiteitOntvanger(new IdentiteitType());
				aanleverBevestiging.getIdentiteitOntvanger().setNummer("nummer");
			}
			aanleverBevestiging.setRolOntvanger(aanleverBericht.getRolOntvanger());
			aanleverBevestiging.setAutorisatieAdres(aanleverBericht.getAutorisatieAdres());
			aanleverBevestiging.setStatuscode("0");
			aanleverBevestiging.setTijdstempelStatus(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
			
			FoutType error = (FoutType)message.getProperty("AANLEVERBERICHT_ERROR",PropertyScope.SESSION);
			if (error == null)
				aanleverBevestiging.setTijdstempelAangeleverd(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
			else
				aanleverBevestiging.setFout(error);

			List<EbMSDataSource> dataSources = new ArrayList<EbMSDataSource>();
			dataSources.add(new EbMSDataSource(name,"application/xml",XMLMessageBuilder.getInstance(BevestigAanleverBericht.class).handle(aanleverBevestiging).getBytes()));

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
