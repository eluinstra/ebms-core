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
package nl.clockwork.ebms.jaxb;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import lombok.val;

public class X509CertificateConverter
{
	public static X509Certificate parseCertificate(byte[] certificate) throws CertificateException
	{
		return decode(certificate);
	}

	public static byte[] printCertificate(X509Certificate certificate) throws CertificateEncodingException
	{
		return certificate.getEncoded();
	}

	private static X509Certificate decode(byte[] certificate) throws CertificateException
	{
		val c = new ByteArrayInputStream(certificate);
		val cf = CertificateFactory.getInstance("X509");
		return (X509Certificate)cf.generateCertificate(c);
	}
}
