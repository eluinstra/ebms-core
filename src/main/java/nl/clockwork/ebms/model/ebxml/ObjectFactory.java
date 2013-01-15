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
package nl.clockwork.ebms.model.ebxml;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the nl.ez.ezpoort.service.model.ebxml package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _RefToMessageId_QNAME = new QName("http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd", "RefToMessageId");
    private final static QName _CPAId_QNAME = new QName("http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd", "CPAId");
    private final static QName _DuplicateElimination_QNAME = new QName("http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd", "DuplicateElimination");
    private final static QName _ConversationId_QNAME = new QName("http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd", "ConversationId");
    private final static QName _Timestamp_QNAME = new QName("http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd", "Timestamp");
    private final static QName _Action_QNAME = new QName("http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd", "Action");
    private final static QName _MessageId_QNAME = new QName("http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd", "MessageId");
    private final static QName _TimeToLive_QNAME = new QName("http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd", "TimeToLive");
    private final static QName _SequenceNumber_QNAME = new QName("http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd", "SequenceNumber");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: nl.ez.ezpoort.service.model.ebxml
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Reference }
     * 
     */
    public Reference createReference() {
        return new Reference();
    }

    /**
     * Create an instance of {@link Schema }
     * 
     */
    public Schema createSchema() {
        return new Schema();
    }

    /**
     * Create an instance of {@link MessageHeader }
     * 
     */
    public MessageHeader createMessageHeader() {
        return new MessageHeader();
    }

    /**
     * Create an instance of {@link Error }
     * 
     */
    public Error createError() {
        return new Error();
    }

    /**
     * Create an instance of {@link Acknowledgment }
     * 
     */
    public Acknowledgment createAcknowledgment() {
        return new Acknowledgment();
    }

    /**
     * Create an instance of {@link Description }
     * 
     */
    public Description createDescription() {
        return new Description();
    }

    /**
     * Create an instance of {@link SequenceNumberType }
     * 
     */
    public SequenceNumberType createSequenceNumberType() {
        return new SequenceNumberType();
    }

    /**
     * Create an instance of {@link To }
     * 
     */
    public To createTo() {
        return new To();
    }

    /**
     * Create an instance of {@link ErrorList }
     * 
     */
    public ErrorList createErrorList() {
        return new ErrorList();
    }

    /**
     * Create an instance of {@link Service }
     * 
     */
    public Service createService() {
        return new Service();
    }

    /**
     * Create an instance of {@link From }
     * 
     */
    public From createFrom() {
        return new From();
    }

    /**
     * Create an instance of {@link MessageData }
     * 
     */
    public MessageData createMessageData() {
        return new MessageData();
    }

    /**
     * Create an instance of {@link SyncReply }
     * 
     */
    public SyncReply createSyncReply() {
        return new SyncReply();
    }

    /**
     * Create an instance of {@link StatusRequest }
     * 
     */
    public StatusRequest createStatusRequest() {
        return new StatusRequest();
    }

    /**
     * Create an instance of {@link StatusResponse }
     * 
     */
    public StatusResponse createStatusResponse() {
        return new StatusResponse();
    }

    /**
     * Create an instance of {@link PartyId }
     * 
     */
    public PartyId createPartyId() {
        return new PartyId();
    }

    /**
     * Create an instance of {@link AckRequested }
     * 
     */
    public AckRequested createAckRequested() {
        return new AckRequested();
    }

    /**
     * Create an instance of {@link Manifest }
     * 
     */
    public Manifest createManifest() {
        return new Manifest();
    }

    /**
     * Create an instance of {@link MessageOrder }
     * 
     */
    public MessageOrder createMessageOrder() {
        return new MessageOrder();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd", name = "RefToMessageId")
    public JAXBElement<String> createRefToMessageId(String value) {
        return new JAXBElement<String>(_RefToMessageId_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd", name = "CPAId")
    public JAXBElement<String> createCPAId(String value) {
        return new JAXBElement<String>(_CPAId_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Object }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd", name = "DuplicateElimination")
    public JAXBElement<String> createDuplicateElimination(String value) {
        return new JAXBElement<String>(_DuplicateElimination_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd", name = "ConversationId")
    public JAXBElement<String> createConversationId(String value) {
        return new JAXBElement<String>(_ConversationId_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link XMLGregorianCalendar }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd", name = "Timestamp")
    public JAXBElement<XMLGregorianCalendar> createTimestamp(XMLGregorianCalendar value) {
        return new JAXBElement<XMLGregorianCalendar>(_Timestamp_QNAME, XMLGregorianCalendar.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd", name = "Action")
    public JAXBElement<String> createAction(String value) {
        return new JAXBElement<String>(_Action_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd", name = "MessageId")
    public JAXBElement<String> createMessageId(String value) {
        return new JAXBElement<String>(_MessageId_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link XMLGregorianCalendar }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd", name = "TimeToLive")
    public JAXBElement<XMLGregorianCalendar> createTimeToLive(XMLGregorianCalendar value) {
        return new JAXBElement<XMLGregorianCalendar>(_TimeToLive_QNAME, XMLGregorianCalendar.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SequenceNumberType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd", name = "SequenceNumber")
    public JAXBElement<SequenceNumberType> createSequenceNumber(SequenceNumberType value) {
        return new JAXBElement<SequenceNumberType>(_SequenceNumber_QNAME, SequenceNumberType.class, null, value);
    }

}
