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
package nl.clockwork.mule.ebms.stub.ebf.processor;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.GregorianCalendar;
import java.util.Properties;

import javax.xml.datatype.DatatypeFactory;

import nl.clockwork.ebms.common.XMLMessageBuilder;
import nl.clockwork.mule.ebms.stub.util.Utils;
import nl.logius.digipoort.ebms._2_0.afleverservice._1.AfleverBericht;
import nl.logius.digipoort.ebms._2_0.afleverservice._1.BerichtBijlagenType;
import nl.logius.digipoort.ebms._2_0.afleverservice._1.BerichtInhoudType;
import nl.logius.digipoort.ebms._2_0.afleverservice._1.IdentiteitType;

import org.apache.commons.io.FileUtils;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transport.PropertyScope;

public class AfleverBerichtGenerator implements Callable
{
	private String baseDir;
	
	@Override
	public Object onCall(MuleEventContext eventContext) throws Exception
	{
		MuleMessage message = eventContext.getMessage();
		if (message.getPayload() instanceof String)
		{
			Properties p = new Properties();
			p.load(new StringReader((String)message.getPayload()));
			String baseDir = this.baseDir + "/" + p.getProperty("baseDir");
			AfleverBericht afleverBericht = new AfleverBericht();
			afleverBericht.setKenmerk(p.getProperty("kenmerk"));
			afleverBericht.setBerichtsoort(p.getProperty("berichtsoort"));
			afleverBericht.setBerichtkenmerk(p.getProperty("berichtkenmerk"));
			afleverBericht.setAanleverkenmerk(p.getProperty("aanleverkenmerk"));
			afleverBericht.setTijdstempelAangeleverd(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
			afleverBericht.setIdentiteitBelanghebbende(new IdentiteitType());
			afleverBericht.getIdentiteitBelanghebbende().setNummer(p.getProperty("identiteitBelanghebbende.nummer"));
			afleverBericht.getIdentiteitBelanghebbende().setType(p.getProperty("identiteitBelanghebbende.type"));
			afleverBericht.setRolBelanghebbende(p.getProperty("rolBelanghebbende"));
			afleverBericht.setIdentiteitOntvanger(new IdentiteitType());
			afleverBericht.getIdentiteitOntvanger().setNummer(p.getProperty("identiteitOntvanger.nummer"));
			afleverBericht.getIdentiteitOntvanger().setType(p.getProperty("identiteitOntvanger.type"));
			afleverBericht.setRolOntvanger(p.getProperty("rolOntvanger"));
			setBerichtInhoud(baseDir,afleverBericht,p.getProperty("berichtInhoud"));
			setBerichtBijlagen(baseDir,afleverBericht,p.getProperty("berichtBijlagen").split(","));
			String result = XMLMessageBuilder.getInstance(AfleverBericht.class).handle(afleverBericht);
			message.setProperty("originalFilename",p.getProperty("berichtInhoud"),PropertyScope.SESSION);
			message.setPayload(result);
		}
		return message;
	}

	private void setBerichtInhoud(String baseDir, AfleverBericht afleverBericht, String fileName) throws IOException
	{
		afleverBericht.setBerichtInhoud(new BerichtInhoudType());
		afleverBericht.getBerichtInhoud().setBestandsnaam(fileName);
		afleverBericht.getBerichtInhoud().setMimeType(Utils.getMimeType(baseDir + "/" + fileName));
		afleverBericht.getBerichtInhoud().setInhoud(FileUtils.readFileToByteArray(new File(baseDir + "/" + fileName)));
	}

	private void setBerichtBijlagen(String baseDir, AfleverBericht afleverBericht, String[] fileNames) throws IOException
	{
		for (String fileName : fileNames)
		{
			afleverBericht.setBerichtBijlagen(new BerichtBijlagenType());
			BerichtInhoudType berichtInhoud = new BerichtInhoudType();
			berichtInhoud.setBestandsnaam(fileName);
			berichtInhoud.setMimeType(Utils.getMimeType(baseDir + "/" + fileName));
			berichtInhoud.setInhoud(FileUtils.readFileToByteArray(new File(baseDir + "/" + fileName)));
			afleverBericht.getBerichtBijlagen().getBijlage().add(berichtInhoud);
		}
	}

	public void setBaseDir(String baseDir)
	{
		this.baseDir = baseDir;
	}
}
