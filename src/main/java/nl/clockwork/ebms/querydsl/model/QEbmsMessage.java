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
package nl.clockwork.ebms.querydsl.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import java.util.*;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QEbmsMessage is a Querydsl query type for QEbmsMessage
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QEbmsMessage extends com.querydsl.sql.RelationalPathBase<QEbmsMessage> {

    private static final long serialVersionUID = -314957467;

    public static final QEbmsMessage ebmsMessage = new QEbmsMessage("ebms_message");

    public final StringPath action = createString("action");

    public final SimplePath<org.w3c.dom.Document> content = createSimple("content", org.w3c.dom.Document.class);

    public final StringPath conversationId = createString("conversationId");

    public final StringPath cpaId = createString("cpaId");

    public final StringPath fromPartyId = createString("fromPartyId");

    public final StringPath fromRole = createString("fromRole");

    public final StringPath messageId = createString("messageId");

    public final NumberPath<Integer> messageNr = createNumber("messageNr", Integer.class);

    public final DateTimePath<java.time.Instant> persistTime = createDateTime("persistTime", java.time.Instant.class);

    public final StringPath refToMessageId = createString("refToMessageId");

    public final StringPath service = createString("service");

    public final NumberPath<Integer> statusRaw = createNumber("status", Integer.class);

    public final EnumPath<nl.clockwork.ebms.EbMSMessageStatus> status = createEnum("status", nl.clockwork.ebms.EbMSMessageStatus.class);

    public final DateTimePath<java.time.Instant> statusTime = createDateTime("statusTime", java.time.Instant.class);

    public final DateTimePath<java.sql.Timestamp> timeStampRaw = createDateTime("timeStamp", java.sql.Timestamp.class);

    public final DateTimePath<java.time.Instant> timeStamp = createDateTime("timeStamp", java.time.Instant.class);

    public final DateTimePath<java.time.Instant> timeToLive = createDateTime("timeToLive", java.time.Instant.class);

    public final StringPath toPartyId = createString("toPartyId");

    public final StringPath toRole = createString("toRole");

    public final com.querydsl.sql.PrimaryKey<QEbmsMessage> sysPk10124 = createPrimaryKey(messageId, messageNr);

    public final com.querydsl.sql.ForeignKey<QEbmsAttachment> _sysFk10141 = createInvForeignKey(Arrays.asList(messageId, messageNr), Arrays.asList("MESSAGE_ID", "MESSAGE_NR"));

    public QEbmsMessage(String variable) {
        super(QEbmsMessage.class, forVariable(variable), "PUBLIC", "ebms_message");
        addMetadata();
    }

    public QEbmsMessage(String variable, String schema, String table) {
        super(QEbmsMessage.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QEbmsMessage(String variable, String schema) {
        super(QEbmsMessage.class, forVariable(variable), schema, "ebms_message");
        addMetadata();
    }

    public QEbmsMessage(Path<? extends QEbmsMessage> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "ebms_message");
        addMetadata();
    }

    public QEbmsMessage(PathMetadata metadata) {
        super(QEbmsMessage.class, metadata, "PUBLIC", "ebms_message");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(action, ColumnMetadata.named("action").withIndex(13).ofType(Types.VARCHAR).withSize(256).notNull());
        addMetadata(content, ColumnMetadata.named("content").withIndex(14).ofType(Types.CLOB).withSize(1073741824));
        addMetadata(conversationId, ColumnMetadata.named("conversation_id").withIndex(3).ofType(Types.VARCHAR).withSize(256).notNull());
        addMetadata(cpaId, ColumnMetadata.named("cpa_id").withIndex(2).ofType(Types.VARCHAR).withSize(256).notNull());
        addMetadata(fromPartyId, ColumnMetadata.named("from_party_id").withIndex(8).ofType(Types.VARCHAR).withSize(256).notNull());
        addMetadata(fromRole, ColumnMetadata.named("from_role").withIndex(9).ofType(Types.VARCHAR).withSize(256));
        addMetadata(messageId, ColumnMetadata.named("message_id").withIndex(4).ofType(Types.VARCHAR).withSize(256).notNull());
        addMetadata(messageNr, ColumnMetadata.named("message_nr").withIndex(5).ofType(Types.SMALLINT).withSize(16).notNull());
        addMetadata(persistTime, ColumnMetadata.named("persist_time").withIndex(17).ofType(Types.TIMESTAMP).withSize(26));
        addMetadata(refToMessageId, ColumnMetadata.named("ref_to_message_id").withIndex(6).ofType(Types.VARCHAR).withSize(256));
        addMetadata(service, ColumnMetadata.named("service").withIndex(12).ofType(Types.VARCHAR).withSize(256).notNull());
        addMetadata(status, ColumnMetadata.named("status").withIndex(15).ofType(Types.SMALLINT).withSize(16));
        addMetadata(statusTime, ColumnMetadata.named("status_time").withIndex(16).ofType(Types.TIMESTAMP).withSize(26));
        addMetadata(timeStamp, ColumnMetadata.named("time_stamp").withIndex(1).ofType(Types.TIMESTAMP).withSize(26).notNull());
        addMetadata(timeToLive, ColumnMetadata.named("time_to_live").withIndex(7).ofType(Types.TIMESTAMP).withSize(26));
        addMetadata(toPartyId, ColumnMetadata.named("to_party_id").withIndex(10).ofType(Types.VARCHAR).withSize(256).notNull());
        addMetadata(toRole, ColumnMetadata.named("to_role").withIndex(11).ofType(Types.VARCHAR).withSize(256));
    }

}

