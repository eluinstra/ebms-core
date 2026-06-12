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
package nl.clockwork.ebms.common.security;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import nl.clockwork.ebms.common.util.SecurityUtils;
import org.apache.commons.io.output.CloseShieldOutputStream;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.xml.security.algorithms.JCEMapper;

/**
 * Streams the {@code <xenc:EncryptedData>} envelope for a binary attachment directly into a {@link CachedOutputStream}, without ever materializing the base64
 * ciphertext in heap. Symmetric counterpart to {@link StreamingXmlDecrypter}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class StreamingXmlEncrypter
{
	private static final String KEY_ALG_URI = "http://www.w3.org/2001/04/xmlenc#rsa-1_5";
	private static final String TYPE_ELEMENT = "http://www.w3.org/2001/04/xmlenc#Element";
	private static final int READ_BUFFER = 16 * 1024;
	private static final SecureRandom RNG = new SecureRandom();

	static CachedOutputStream encrypt(InputStream plaintext, X509Certificate certificate, String dataAlgUri, String contentId, String mimeType)
			throws GeneralSecurityException, IOException
	{
		val secretKey = SecurityUtils.generateKey(dataAlgUri);
		val wrappedKeyB64 = wrapKey(secretKey, certificate);
		val ivLen = JCEMapper.getIVLengthFromURI(dataAlgUri) / 8;
		val iv = new byte[ivLen];
		RNG.nextBytes(iv);
		val dataCipher = initDataCipher(dataAlgUri, secretKey, iv);

		val out = new CachedOutputStream();
		try
		{
			writeHeader(out, contentId, mimeType, dataAlgUri, certificate, wrappedKeyB64);
			streamCiphertext(out, plaintext, dataCipher, iv);
			writeFooter(out);
			out.lockOutputStream();
			return out;
		}
		catch (IOException | RuntimeException e)
		{
			out.close();
			throw e;
		}
	}

	private static String wrapKey(SecretKey secretKey, X509Certificate certificate) throws GeneralSecurityException
	{
		val rsaJce = JCEMapper.translateURItoJCEID(KEY_ALG_URI);
		if (rsaJce == null)
			throw new GeneralSecurityException("Unsupported key wrap URI: " + KEY_ALG_URI);
		val rsa = Cipher.getInstance(rsaJce);
		rsa.init(Cipher.WRAP_MODE, certificate.getPublicKey());
		return Base64.getEncoder().encodeToString(rsa.wrap(secretKey));
	}

	private static Cipher initDataCipher(String dataAlgUri, SecretKey secretKey, byte[] iv) throws GeneralSecurityException
	{
		val transformation = JCEMapper.translateURItoJCEID(dataAlgUri);
		if (transformation == null)
			throw new GeneralSecurityException("Unsupported data algorithm URI: " + dataAlgUri);
		val cipher = Cipher.getInstance(transformation);
		val spec = transformation.contains("GCM") ? new GCMParameterSpec(128, iv) : new IvParameterSpec(iv);
		cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
		return cipher;
	}

	private static void writeHeader(OutputStream out, String contentId, String mimeType, String dataAlgUri, X509Certificate certificate, String wrappedKeyB64)
			throws IOException
	{
		val keyName = certificate.getSubjectX500Principal().getName();
		val sb = new StringBuilder(1024);
		sb.append("<xenc:EncryptedData xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\"");
		sb.append(" Id=\"").append(xmlAttr(contentId)).append('"');
		if (mimeType != null)
			sb.append(" MimeType=\"").append(xmlAttr(mimeType)).append('"');
		sb.append(" Type=\"").append(TYPE_ELEMENT).append("\">");
		sb.append("<xenc:EncryptionMethod Algorithm=\"").append(xmlAttr(dataAlgUri)).append("\"/>");
		sb.append("<ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">");
		sb.append("<xenc:EncryptedKey>");
		sb.append("<xenc:EncryptionMethod Algorithm=\"").append(KEY_ALG_URI).append("\"/>");
		sb.append("<ds:KeyInfo><ds:KeyName>").append(xmlText(keyName)).append("</ds:KeyName></ds:KeyInfo>");
		sb.append("<xenc:CipherData><xenc:CipherValue>").append(wrappedKeyB64).append("</xenc:CipherValue></xenc:CipherData>");
		sb.append("</xenc:EncryptedKey>");
		sb.append("</ds:KeyInfo>");
		sb.append("<xenc:CipherData><xenc:CipherValue>");
		out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
	}

	private static void writeFooter(OutputStream out) throws IOException
	{
		out.write("</xenc:CipherValue></xenc:CipherData></xenc:EncryptedData>".getBytes(StandardCharsets.UTF_8));
	}

	private static void streamCiphertext(OutputStream out, InputStream plaintext, Cipher dataCipher, byte[] iv) throws IOException
	{
		// java.util.Base64.getEncoder() emits unbroken base64 (no line wrapping).
		try (val b64 = Base64.getEncoder().wrap(CloseShieldOutputStream.wrap(out)))
		{
			b64.write(iv);
			val buf = new byte[READ_BUFFER];
			int n;
			while ((n = plaintext.read(buf)) > 0)
			{
				val update = dataCipher.update(buf, 0, n);
				if (update != null && update.length > 0)
					b64.write(update);
			}
			try
			{
				val tail = dataCipher.doFinal();
				if (tail != null && tail.length > 0)
					b64.write(tail);
			}
			catch (GeneralSecurityException e)
			{
				throw new IOException(e);
			}
		}
	}

	private static String xmlAttr(String s)
	{
		return escape(s, true);
	}

	private static String xmlText(String s)
	{
		return escape(s, false);
	}

	private static String escape(String s, boolean attr)
	{
		val out = new StringBuilder(s.length() + 16);
		for (int i = 0; i < s.length(); i++)
		{
			val c = s.charAt(i);
			switch (c)
			{
				case '&' -> out.append("&amp;");
				case '<' -> out.append("&lt;");
				case '>' -> out.append("&gt;");
				case '"' -> out.append(attr ? "&quot;" : "\"");
				case '\'' -> out.append(attr ? "&apos;" : "'");
				default -> out.append(c);
			}
		}
		return out.toString();
	}
}
