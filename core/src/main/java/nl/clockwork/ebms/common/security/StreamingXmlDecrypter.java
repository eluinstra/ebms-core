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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.val;
import nl.clockwork.ebms.common.util.ValidationException;
import org.apache.commons.codec.binary.Base64;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.xml.security.algorithms.JCEMapper;
import org.apache.xml.security.utils.EncryptionConstants;

/**
 * Streams a Santuario-produced {@code <xenc:EncryptedData>} envelope, unwrapping the symmetric key from {@code <xenc:EncryptedKey>} and decrypting the outer
 * {@code <xenc:CipherValue>} text incrementally into a {@link CachedOutputStream}. Peak heap is bounded by the {@code CachedOutputStream} threshold plus StAX
 * parser buffers, independent of attachment size.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class StreamingXmlDecrypter
{
	private static final String XENC_NS = EncryptionConstants.EncryptionSpecNS;
	private static final String DEFAULT_KEY_ALG_URI = "http://www.w3.org/2001/04/xmlenc#rsa-1_5";

	@Value
	static class Result
	{
		CachedOutputStream content;
		String mimeType;
	}

	static Result decrypt(InputStream encryptedXml, PrivateKey kek) throws XMLStreamException, IOException, GeneralSecurityException, ValidationException
	{
		val xif = hardenedInputFactory();
		val reader = xif.createXMLStreamReader(encryptedXml);
		try
		{
			return new Walker(reader, kek).run();
		}
		finally
		{
			reader.close();
		}
	}

	private static XMLInputFactory hardenedInputFactory()
	{
		val xif = XMLInputFactory.newInstance();
		xif.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
		xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
		xif.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.FALSE);
		xif.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
		xif.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
		return xif;
	}

	private static final class Walker
	{
		private final XMLStreamReader reader;
		private final PrivateKey kek;

		private boolean seenEncryptedData;
		private String mimeType;
		private int encryptedDataDepth = -1;
		private int depth;
		private boolean inEncryptedKey;
		private boolean inEncryptedKeyCipherValue;
		private boolean inOuterCipherValue;
		private String dataAlgUri;
		private String keyAlgUri = DEFAULT_KEY_ALG_URI;

		private final StringBuilder keyCipherText = new StringBuilder(2048);
		private SecretKey dataKey;

		private final StringBuilder b64Carry = new StringBuilder(4096);
		private CachedOutputStream out;
		private Cipher dataCipher;
		private int ivLen;
		private final ByteArrayOutputStream ivBuf = new ByteArrayOutputStream(16);

		Walker(XMLStreamReader reader, PrivateKey kek)
		{
			this.reader = reader;
			this.kek = kek;
		}

		Result run() throws XMLStreamException, IOException, GeneralSecurityException, ValidationException
		{
			while (reader.hasNext())
			{
				val ev = reader.next();
				switch (ev)
				{
					case XMLStreamConstants.START_ELEMENT:
						depth++;
						handleStart();
						break;
					case XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA:
						handleText();
						break;
					case XMLStreamConstants.END_ELEMENT:
						handleEnd();
						depth--;
						break;
					default:
						break;
				}
			}
			if (!seenEncryptedData)
				throw new ValidationException("EncryptedData element not found");
			if (out == null)
				throw new ValidationException("EncryptedData has no CipherValue");
			return new Result(out, mimeType);
		}

		private boolean isXenc(String localName)
		{
			return XENC_NS.equals(reader.getNamespaceURI()) && localName.equals(reader.getLocalName());
		}

		private void handleStart()
		{
			if (encryptedDataDepth < 0)
			{
				if (isXenc(EncryptionConstants._TAG_ENCRYPTEDDATA))
				{
					encryptedDataDepth = depth;
					seenEncryptedData = true;
					mimeType = reader.getAttributeValue(null, "MimeType");
				}
				return;
			}
			if (isXenc(EncryptionConstants._TAG_ENCRYPTIONMETHOD))
			{
				val alg = reader.getAttributeValue(null, "Algorithm");
				if (inEncryptedKey)
					keyAlgUri = alg;
				else if (depth == encryptedDataDepth + 1)
					dataAlgUri = alg;
			}
			else if (isXenc(EncryptionConstants._TAG_ENCRYPTEDKEY))
			{
				inEncryptedKey = true;
			}
			else if (isXenc(EncryptionConstants._TAG_CIPHERVALUE))
			{
				if (inEncryptedKey)
					inEncryptedKeyCipherValue = true;
				else
					inOuterCipherValue = true;
			}
		}

		private void handleText() throws GeneralSecurityException, IOException
		{
			if (inEncryptedKeyCipherValue)
				keyCipherText.append(reader.getText());
			else if (inOuterCipherValue)
				feedBase64(reader.getText());
		}

		private void handleEnd() throws GeneralSecurityException, IOException
		{
			if (encryptedDataDepth < 0)
				return;
			if (isXenc(EncryptionConstants._TAG_CIPHERVALUE))
			{
				if (inEncryptedKeyCipherValue)
					inEncryptedKeyCipherValue = false;
				else if (inOuterCipherValue)
				{
					finishOuterCipherValue();
					inOuterCipherValue = false;
				}
			}
			else if (isXenc(EncryptionConstants._TAG_ENCRYPTEDKEY))
			{
				unwrapDataKey();
				inEncryptedKey = false;
			}
			else if (isXenc(EncryptionConstants._TAG_ENCRYPTEDDATA) && depth == encryptedDataDepth)
			{
				encryptedDataDepth = -1;
			}
		}

		private void unwrapDataKey() throws GeneralSecurityException
		{
			if (dataAlgUri == null)
				throw new GeneralSecurityException("Missing data EncryptionMethod Algorithm");
			val wrapped = Base64.decodeBase64(keyCipherText.toString());
			keyCipherText.setLength(0);
			val keyJce = JCEMapper.translateURItoJCEID(keyAlgUri);
			val dataJceKey = JCEMapper.getJCEKeyAlgorithmFromURI(dataAlgUri);
			if (keyJce == null || dataJceKey == null)
				throw new GeneralSecurityException("Unsupported algorithm URI: " + keyAlgUri + " / " + dataAlgUri);
			val unwrap = Cipher.getInstance(keyJce);
			unwrap.init(Cipher.UNWRAP_MODE, kek);
			dataKey = (SecretKey)unwrap.unwrap(wrapped, dataJceKey, Cipher.SECRET_KEY);
		}

		private void feedBase64(String text) throws GeneralSecurityException, IOException
		{
			for (int i = 0; i < text.length(); i++)
			{
				val c = text.charAt(i);
				if (!Character.isWhitespace(c))
					b64Carry.append(c);
			}
			// Decode only aligned 4-char groups; carry the trailing 0–3 chars for the next call.
			val aligned = b64Carry.length() & ~3;
			if (aligned == 0)
				return;
			val chunk = b64Carry.substring(0, aligned);
			b64Carry.delete(0, aligned);
			consumeCiphertext(Base64.decodeBase64(chunk));
		}

		private void finishOuterCipherValue() throws GeneralSecurityException, IOException
		{
			if (!b64Carry.isEmpty())
			{
				val tail = Base64.decodeBase64(b64Carry.toString());
				b64Carry.setLength(0);
				consumeCiphertext(tail);
			}
			if (dataCipher == null)
				throw new GeneralSecurityException("Ciphertext shorter than IV length");
			val finalBytes = dataCipher.doFinal();
			if (finalBytes != null && finalBytes.length > 0)
				out.write(finalBytes);
			out.lockOutputStream();
		}

		private void consumeCiphertext(byte[] data) throws GeneralSecurityException, IOException
		{
			if (data.length == 0)
				return;
			if (dataCipher == null)
			{
				if (out == null)
					out = new CachedOutputStream();
				if (ivLen == 0)
					ivLen = JCEMapper.getIVLengthFromURI(dataAlgUri) / 8;
				val needed = ivLen - ivBuf.size();
				val take = Math.min(needed, data.length);
				ivBuf.write(data, 0, take);
				if (ivBuf.size() < ivLen)
					return;
				initDataCipher(ivBuf.toByteArray());
				if (take == data.length)
					return;
				val update = dataCipher.update(data, take, data.length - take);
				if (update != null && update.length > 0)
					out.write(update);
				return;
			}
			val update = dataCipher.update(data);
			if (update != null && update.length > 0)
				out.write(update);
		}

		private void initDataCipher(byte[] iv) throws GeneralSecurityException
		{
			val transformation = JCEMapper.translateURItoJCEID(dataAlgUri);
			if (transformation == null)
				throw new GeneralSecurityException("Unsupported data algorithm URI: " + dataAlgUri);
			dataCipher = Cipher.getInstance(transformation);
			val spec = transformation.contains("GCM") ? new GCMParameterSpec(128, iv) : new IvParameterSpec(iv);
			dataCipher.init(Cipher.DECRYPT_MODE, dataKey, spec);
		}
	}
}
