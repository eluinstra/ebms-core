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
package nl.clockwork.ebms.model;

import java.security.cert.X509Certificate;
import java.util.List;

import nl.clockwork.ebms.model.xml.dsig.KeyInfoType;
import nl.clockwork.ebms.model.xml.dsig.ObjectType;
import nl.clockwork.ebms.model.xml.dsig.SignatureType;
import nl.clockwork.ebms.model.xml.dsig.SignatureValueType;
import nl.clockwork.ebms.model.xml.dsig.SignedInfoType;

public class Signature extends SignatureType
{
	private X509Certificate certificate;
	private SignatureType signature;
	private boolean isValid;

	public Signature(X509Certificate certificate, SignatureType signature, boolean isValid)
	{
		this.certificate = certificate;
		this.signature = signature;
		this.isValid = isValid;
	}
	
	public X509Certificate getCertificate()
	{
		return certificate;
	}
	
	public SignatureType getSignature()
	{
		return signature;
	}
	
	public boolean isValid()
	{
		return isValid;
	}
	
	@Override
	public String getId()
	{
		return signature.getId();
	}
	
	@Override
	public KeyInfoType getKeyInfo()
	{
		return signature.getKeyInfo();
	}
	
	@Override
	public List<ObjectType> getObject()
	{
		return signature.getObject();
	}
	
	@Override
	public SignatureValueType getSignatureValue()
	{
		return signature.getSignatureValue();
	}
	
	@Override
	public SignedInfoType getSignedInfo()
	{
		return signature.getSignedInfo();
	}
	
	
}
