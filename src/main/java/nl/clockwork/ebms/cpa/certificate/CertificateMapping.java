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
package nl.clockwork.ebms.cpa.certificate;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.function.Function;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.jaxb.X509CertificateAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class CertificateMapping implements Serializable
{
	private static final long serialVersionUID = 1L;
	public static final Function<X509Certificate,String> getId = c -> "issuer=" + c.getIssuerX500Principal().getName() + "; serialNr=" + c.getSerialNumber().toString();
	@XmlElement(required=true)
	@XmlJavaTypeAdapter(X509CertificateAdapter.class)
	@XmlSchemaType(name="base64Binary")
	@NonNull
	X509Certificate source;
	@XmlElement(required=true)
	@XmlJavaTypeAdapter(X509CertificateAdapter.class)
	@XmlSchemaType(name="base64Binary")
	@NonNull
	X509Certificate destination;
	@XmlElement
	String cpaId;

	public String getId()
	{
		return getId.apply(source);
	}

	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + "(" + printSource() + ", " + printDestination() + ")";
	}

	private String printSource()
	{
		return "source=" + source.getSubjectDN() + "(" + getId.apply(source) + ")";
	}
	private String printDestination()
	{
		return "destination=" + destination.getSubjectDN() + "(" + getId.apply(destination) + ")";
	}
}
