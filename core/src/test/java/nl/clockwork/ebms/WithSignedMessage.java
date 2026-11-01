/*
 * Copyright 2011 - 2026 Clockwork
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
package nl.clockwork.ebms;

import static nl.clockwork.ebms.api.cpa.CPATestUtils.cpaCache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpRequest.BodyPublisher;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.val;
import nl.clockwork.ebms.api.ebms.model.DataSource;
import nl.clockwork.ebms.api.ebms.model.MessageRequest;
import nl.clockwork.ebms.api.ebms.model.MessageRequestProperties;
import nl.clockwork.ebms.api.ebms.model.Party;
import nl.clockwork.ebms.client.delivery.client.MultipartBodyPublisher;
import nl.clockwork.ebms.common.cpa.CPAManager;
import nl.clockwork.ebms.common.cpa.CPARepository;
import nl.clockwork.ebms.encryption.EbMSMessageEncrypter;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.security.EbMSKeyStore;
import nl.clockwork.ebms.security.EbMSTrustStore;
import nl.clockwork.ebms.security.KeyStoreType;
import nl.clockwork.ebms.signing.EbMSSignatureGenerator;
import nl.clockwork.ebms.util.DOMUtils;
import org.apache.xml.security.Init;
import org.mockito.Mockito;

/**
 * Client-side helper that builds EbMS messages, signs and (optionally) encrypts them the same way the
 * outbound delivery path does, and serializes them to the wire format accepted by the {@code /ebms} endpoint.
 * Used by end-to-end ITs that exercise a CPA with non-repudiation (signature) and/or confidentiality
 * (encryption) enabled over plain HTTP (no TLS client certificate).
 */
public interface WithSignedMessage
{
        KeyStoreType keyStoreType = KeyStoreType.PKCS12;
        String keyStorePath = "nl/clockwork/ebms/keystore.p12";
        String keyStorePassword = "password";

        default void initXmlSecurity()
        {
                Init.init();
        }

        default CPAManager cpaManagerFor(String cpaId) throws GeneralSecurityException, IOException
        {
                val cpaRepository = Mockito.mock(CPARepository.class);
                Mockito.when(cpaRepository.getCPA(cpaId)).thenReturn(cpaCache.apply(cpaId));
                return new CPAManager(
                                cpaRepository,
                                (cpa, certificate) -> certificate,
                                url -> url,
                                EbMSKeyStore.of(keyStoreType, keyStorePath, keyStorePassword, keyStorePassword),
                                false);
        }

        default EbMSMessageFactory messageFactory(CPAManager cpaManager)
        {
                return new EbMSMessageFactory(cpaManager, new EbMSIdGenerator());
        }

        default EbMSSignatureGenerator signatureGenerator(CPAManager cpaManager) throws GeneralSecurityException, IOException
        {
                return new EbMSSignatureGenerator(
                                cpaManager,
                                EbMSKeyStore.of(keyStoreType, keyStorePath, keyStorePassword, keyStorePassword));
        }

        default EbMSMessageEncrypter messageEncrypter(CPAManager cpaManager) throws GeneralSecurityException, IOException
        {
                return new EbMSMessageEncrypter(
                                cpaManager,
                                EbMSTrustStore.of(keyStoreType, keyStorePath, keyStorePassword));
        }

        /**
         * Builds, signs and (when {@code encrypt}) encrypts an EbMS message from the given CPA. The message is signed
         * over the plaintext attachment first and only then encrypted, matching the outbound delivery path
         * (sign at creation, encrypt at delivery): the receive side validates the signature's attachment reference
         * against the decrypted bytes, so the digest must be computed over the plaintext. The stored message id is
         * {@code messageId + "@" + hostname}.
         */
        default EbMSDocument buildSignedEbMSMessage(
                        String cpaId,
                        String messageId,
                        String fromPartyId,
                        String fromRole,
                        String service,
                        String action,
                        boolean encrypt,
                        byte[] attachment) throws Exception
        {
                initXmlSecurity();
                val cpaManager = cpaManagerFor(cpaId);
                val properties = new MessageRequestProperties(cpaId, new Party(fromPartyId, fromRole), service, action);
                properties.setMessageId(messageId);
                val message = messageFactory(cpaManager)
                                .createEbMSMessage(
                                                MessageRequest.builder()
                                                                .properties(properties)
                                                                .dataSources(attachment == null ? List.of() : List.of(new DataSource("test.txt", null, "text/plain; charset=UTF-8", attachment)))
                                                                .build());
                val document = EbMSMessageUtils.getEbMSDocument(message);
                signatureGenerator(cpaManager).generate(document, message);
                if (encrypt)
                        messageEncrypter(cpaManager).encrypt(message);
                return document;
        }

        /**
         * A serializable, already-serialized wire payload ready to POST to {@code /ebms}.
         */
        record WirePayload(byte[] body, String contentType)
        {
        }

        /**
         * Serializes the document to the wire format accepted by the {@code /ebms} endpoint using the same
         * {@link MultipartBodyPublisher} the outbound delivery path uses, so the multipart body on the wire is
         * byte-identical to what the signature was generated over (the signed attachment reference must match).
         */
        default WirePayload toWire(EbMSDocument document) throws Exception
        {
                val message = DOMUtils.toString(document.getMessage(), "UTF-8");
                if (document.getAttachments().isEmpty())
                        return new WirePayload(message.getBytes(StandardCharsets.UTF_8), "text/xml; charset=UTF-8");
                val publisher = new MultipartBodyPublisher(document.getContentId()).addXml(document.getContentId(), message);
                document.getAttachments().forEach(publisher::addAttachment);
                return new WirePayload(bodyBytes(publisher), publisher.contentType());
        }

        /**
         * Drains a {@link BodyPublisher} (backed by the multipart channel) into a single byte array.
         */
        private static byte[] bodyBytes(BodyPublisher publisher) throws InterruptedException
        {
                val buffer = new ByteArrayOutputStream();
                val latch = new CountDownLatch(1);
                val error = new AtomicReference<Throwable>();
                publisher.subscribe(new Subscriber<>()
                {
                        private Subscription subscription;

                        @Override
                        public void onSubscribe(Subscription subscription)
                        {
                                this.subscription = subscription;
                                subscription.request(1024);
                        }

                        @Override
                        public void onNext(ByteBuffer byteBuffer)
                        {
                                val data = new byte[byteBuffer.remaining()];
                                byteBuffer.get(data);
                                try
                                {
                                        buffer.write(data);
                                        subscription.request(1024);
                                }
                                catch (IOException e)
                                {
                                        error.set(e);
                                        latch.countDown();
                                }
                        }

                        @Override
                        public void onError(Throwable throwable)
                        {
                                error.set(throwable);
                                latch.countDown();
                        }

                        @Override
                        public void onComplete()
                        {
                                latch.countDown();
                        }
                });
                if (!latch.await(30, TimeUnit.SECONDS))
                        throw new IllegalStateException("Timed out reading multipart body");
                val cause = error.get();
                if (cause != null)
                        throw new IllegalStateException(cause);
                return buffer.toByteArray();
        }
}
