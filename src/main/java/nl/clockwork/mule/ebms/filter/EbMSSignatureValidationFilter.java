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
package nl.clockwork.mule.ebms.filter;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;

import nl.clockwork.common.dao.DAOException;
import nl.clockwork.mule.ebms.Constants;
import nl.clockwork.mule.ebms.dao.EbMSDAO;
import nl.clockwork.mule.ebms.model.EbMSBaseMessage;
import nl.clockwork.mule.ebms.model.Signature;
import nl.clockwork.mule.ebms.model.cpp.cpa.Certificate;
import nl.clockwork.mule.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.mule.ebms.model.cpp.cpa.DeliveryChannel;
import nl.clockwork.mule.ebms.model.cpp.cpa.DocExchange;
import nl.clockwork.mule.ebms.model.cpp.cpa.KeyValueType;
import nl.clockwork.mule.ebms.model.cpp.cpa.RSAKeyValueType;
import nl.clockwork.mule.ebms.model.cpp.cpa.X509DataType;
import nl.clockwork.mule.ebms.model.ebxml.MessageHeader;
import nl.clockwork.mule.ebms.util.CPAUtils;
import nl.clockwork.mule.ebms.util.EbMSMessageUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;

public class EbMSSignatureValidationFilter implements Filter
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private EbMSDAO ebMSDAO;
	private boolean isSigned = true;

	@Override
	public boolean accept(MuleMessage message)
	{
		if (message.getPayload() instanceof EbMSBaseMessage)
		{
			try
			{
				if (isSigned)
				{
					EbMSBaseMessage msg = (EbMSBaseMessage)message.getPayload();
					MessageHeader messageHeader = msg.getMessageHeader();
					CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(messageHeader.getCPAId());
					Signature signature = (Signature)message.getProperty(Constants.EBMS_SIGNATURE);
					if (signature == null)
					{
						message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/Signature",Constants.EbMSErrorCode.SECURITY_FAILURE.errorCode(),"No signature found."));
						return false;
					}
					//List<Certificate> certificates = CPAUtils.getCertificates(cpa,messageHeader);
					//Certificate certificate = CPAUtils.getCertificateById(cpa,messageHeader,"OVERHEID_SigningCert");
//					List l = CPAUtils.getCanSend(
//							CPAUtils.getCollaborationRoles(cpa,messageHeader.getFrom().getPartyId().get(0).getType(),messageHeader.getFrom().getPartyId().get(0).getValue(),messageHeader.getFrom().getRole()),
//							messageHeader.getService().getType(),
//							messageHeader.getService().getValue(),
//							messageHeader.getAction()
//					).getThisPartyActionBinding().getChannelId();
//					DeliveryChannel channel = (DeliveryChannel)getElement(l,"ChannelId");
//					Certificate certificate = ((Certificate)((DocExchange)channel.getDocExchangeId()).getEbXMLSenderBinding().getSenderNonRepudiation().getSigningCertificateRef().getCertId());
//					//if (!checkCertificate(certificates,signature))
//					if (!checkCertificate(certificate,signature))
//					{
//					//	message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/Signature",Constants.EbMSErrorCode.SECURITY_FAILURE.errorCode(),"Certificate invalid."));
//					//	return false;
//					}
					if (!signature.isValid())
					{
						message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/Signature",Constants.EbMSErrorCode.SECURITY_FAILURE.errorCode(),"Signature invalid."));
						return false;
					}
				}
				return true;
			}
			catch (DAOException e)
			{
				throw new RuntimeException(e);
			}
		}
		return true;
	}

	private boolean checkCertificate(Certificate certificate, Signature signature)
	{
		List<KeyValueType> keyValueTypes = (List<KeyValueType>)getElements(certificate.getKeyInfo().getContent(),"KeyValue");
		for (KeyValueType keyValueType : keyValueTypes)
		{
			List<RSAKeyValueType> rsaKeyValueType = (List<RSAKeyValueType>)getElements(keyValueType.getContent(),"RSAKeyValue");
			//rsaKeyValueType.
		}
		List<X509DataType> x509DataType = (List<X509DataType>)getElements(certificate.getKeyInfo().getContent(),"X509Data");
		//X509Certi x509DataType.
		return true;
		//return false;
	}

	private Object getElement(List<Object> elements, String name)
	{
		for(Object element : elements)
			if (element instanceof JAXBElement<?>)
				if (name.equals(((JAXBElement<?>)element).getName().getLocalPart()))
					return ((JAXBElement<?>)element).getValue();
		return null;
	}

	private List<? extends Object> getElements(List<Object> elements, String name)
	{
		List<Object> result = new ArrayList<Object>();
		for(Object element : elements)
			if (element instanceof JAXBElement<?>)
				if (name.equals(((JAXBElement<?>)element).getName().getLocalPart()))
					result.add(((JAXBElement<?>)element).getValue());
		return result;
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}

	public void setSigned(boolean isSigned)
	{
		this.isSigned = isSigned;
	}
}
