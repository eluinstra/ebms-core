/*
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
package nl.clockwork.ebms.api.cpa.certificate;

import static nl.clockwork.ebms.common.cpa.certificate.X509CertificateConverter.parseCertificate;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;

import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
class CertificateMapping
{
	@NonNull
	String source;
	@NonNull
	String destination;
	String cpaId;

	static CertificateMapping of(nl.clockwork.ebms.common.cpa.certificate.CertificateMapping m)
	{
		try
		{
			return new CertificateMapping(encodeBase64String(m.getSource().getEncoded()), encodeBase64String(m.getDestination().getEncoded()), m.getCpaId());
		}
		catch (CertificateEncodingException e)
		{
			throw new IllegalStateException(e);
		}
	}

	nl.clockwork.ebms.common.cpa.certificate.CertificateMapping toCertificateMapping()
	{
		try
		{
			return new nl.clockwork.ebms.common.cpa.certificate.CertificateMapping(
					parseCertificate(decodeBase64(source)),
					parseCertificate(decodeBase64(destination)),
					cpaId);
		}
		catch (CertificateException e)
		{
			throw new IllegalStateException(e);
		}
	}
}