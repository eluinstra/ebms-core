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

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

import java.sql.Types;

import javax.annotation.Generated;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;




/**
 * QEbmsEvent is a Querydsl query type for QEbmsEvent
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QEbmsEvent extends com.querydsl.sql.RelationalPathBase<QEbmsEvent> {

    private static final long serialVersionUID = 462050168;

    public static final QEbmsEvent ebmsEvent = new QEbmsEvent("EBMS_EVENT");

    public final StringPath cpaId = createString("cpaId");

    public final BooleanPath isConfidential = createBoolean("isConfidential");

    public final StringPath messageId = createString("messageId");

    public final StringPath receiveChannelId = createString("receiveChannelId");

    public final NumberPath<Integer> retries = createNumber("retries", Integer.class);

    public final StringPath sendChannelId = createString("sendChannelId");

    public final StringPath serverId = createString("serverId");

    public final DateTimePath<java.sql.Timestamp> timeStampRaw = createDateTime("timeStamp", java.sql.Timestamp.class);

    public final DateTimePath<java.time.Instant> timeStamp = createDateTime("timeStamp", java.time.Instant.class);

    public final DateTimePath<java.time.Instant> timeToLive = createDateTime("timeToLive", java.time.Instant.class);

    public QEbmsEvent(String variable) {
        super(QEbmsEvent.class, forVariable(variable), "PUBLIC", "EBMS_EVENT");
        addMetadata();
    }

    public QEbmsEvent(String variable, String schema, String table) {
        super(QEbmsEvent.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QEbmsEvent(String variable, String schema) {
        super(QEbmsEvent.class, forVariable(variable), schema, "EBMS_EVENT");
        addMetadata();
    }

    public QEbmsEvent(Path<? extends QEbmsEvent> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "EBMS_EVENT");
        addMetadata();
    }

    public QEbmsEvent(PathMetadata metadata) {
        super(QEbmsEvent.class, metadata, "PUBLIC", "EBMS_EVENT");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(cpaId, ColumnMetadata.named("CPA_ID").withIndex(1).ofType(Types.VARCHAR).withSize(256).notNull());
        addMetadata(isConfidential, ColumnMetadata.named("IS_CONFIDENTIAL").withIndex(7).ofType(Types.BOOLEAN).withSize(0).notNull());
        addMetadata(messageId, ColumnMetadata.named("MESSAGE_ID").withIndex(3).ofType(Types.VARCHAR).withSize(256).notNull());
        addMetadata(receiveChannelId, ColumnMetadata.named("RECEIVE_CHANNEL_ID").withIndex(2).ofType(Types.VARCHAR).withSize(256).notNull());
        addMetadata(retries, ColumnMetadata.named("RETRIES").withIndex(6).ofType(Types.SMALLINT).withSize(16).notNull());
        addMetadata(sendChannelId, ColumnMetadata.named("SEND_CHANNEL_ID").withIndex(9).ofType(Types.VARCHAR).withSize(256));
        addMetadata(serverId, ColumnMetadata.named("SERVER_ID").withIndex(8).ofType(Types.VARCHAR).withSize(256));
        addMetadata(timeStamp, ColumnMetadata.named("TIME_STAMP").withIndex(5).ofType(Types.TIMESTAMP).withSize(26).notNull());
        addMetadata(timeToLive, ColumnMetadata.named("TIME_TO_LIVE").withIndex(4).ofType(Types.TIMESTAMP).withSize(26));
    }

}

