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

import com.querydsl.sql.ColumnMetadata;

import java.sql.Types;




/**
 * QEbmsMessageEvent is a Querydsl query type for QEbmsMessageEvent
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QEbmsMessageEvent extends com.querydsl.sql.RelationalPathBase<QEbmsMessageEvent> {

    private static final long serialVersionUID = 1196390453;

    public static final QEbmsMessageEvent ebmsMessageEvent = new QEbmsMessageEvent("ebms_message_event");

    public final EnumPath<nl.clockwork.ebms.event.listener.EbMSMessageEventType> eventType = createEnum("eventType", nl.clockwork.ebms.event.listener.EbMSMessageEventType.class);

    public final StringPath messageId = createString("messageId");

    public final BooleanPath processed = createBoolean("processed");

    public final DateTimePath<java.sql.Timestamp> timeStamp = createDateTime("timeStamp", java.sql.Timestamp.class);

    public QEbmsMessageEvent(String variable) {
        super(QEbmsMessageEvent.class, forVariable(variable), "PUBLIC", "ebms_message_event");
        addMetadata();
    }

    public QEbmsMessageEvent(String variable, String schema, String table) {
        super(QEbmsMessageEvent.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QEbmsMessageEvent(String variable, String schema) {
        super(QEbmsMessageEvent.class, forVariable(variable), schema, "ebms_message_event");
        addMetadata();
    }

    public QEbmsMessageEvent(Path<? extends QEbmsMessageEvent> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "ebms_message_event");
        addMetadata();
    }

    public QEbmsMessageEvent(PathMetadata metadata) {
        super(QEbmsMessageEvent.class, metadata, "PUBLIC", "ebms_message_event");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(eventType, ColumnMetadata.named("event_type").withIndex(2).ofType(Types.SMALLINT).withSize(16).notNull());
        addMetadata(messageId, ColumnMetadata.named("message_id").withIndex(1).ofType(Types.VARCHAR).withSize(256).notNull());
        addMetadata(processed, ColumnMetadata.named("processed").withIndex(4).ofType(Types.SMALLINT).withSize(16).notNull());
        addMetadata(timeStamp, ColumnMetadata.named("time_stamp").withIndex(3).ofType(Types.TIMESTAMP).withSize(26).notNull());
    }

}

