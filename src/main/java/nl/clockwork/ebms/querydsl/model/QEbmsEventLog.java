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
 * QEbmsEventLog is a Querydsl query type for QEbmsEventLog
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QEbmsEventLog extends com.querydsl.sql.RelationalPathBase<QEbmsEventLog> {

    private static final long serialVersionUID = -433552212;

    public static final QEbmsEventLog ebmsEventLog = new QEbmsEventLog("EBMS_EVENT_LOG");

    public final StringPath errorMessage = createString("errorMessage");

    public final StringPath messageId = createString("messageId");

    public final EnumPath<nl.clockwork.ebms.event.processor.EbMSEventStatus> status = createEnum("status", nl.clockwork.ebms.event.processor.EbMSEventStatus.class);

    public final DateTimePath<java.time.Instant> timeStamp = createDateTime("timeStamp", java.time.Instant.class);

    public final StringPath uri = createString("uri");

    public QEbmsEventLog(String variable) {
        super(QEbmsEventLog.class, forVariable(variable), "PUBLIC", "EBMS_EVENT_LOG");
        addMetadata();
    }

    public QEbmsEventLog(String variable, String schema, String table) {
        super(QEbmsEventLog.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QEbmsEventLog(String variable, String schema) {
        super(QEbmsEventLog.class, forVariable(variable), schema, "EBMS_EVENT_LOG");
        addMetadata();
    }

    public QEbmsEventLog(Path<? extends QEbmsEventLog> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "EBMS_EVENT_LOG");
        addMetadata();
    }

    public QEbmsEventLog(PathMetadata metadata) {
        super(QEbmsEventLog.class, metadata, "PUBLIC", "EBMS_EVENT_LOG");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(errorMessage, ColumnMetadata.named("ERROR_MESSAGE").withIndex(5).ofType(Types.CLOB).withSize(1073741824));
        addMetadata(messageId, ColumnMetadata.named("MESSAGE_ID").withIndex(1).ofType(Types.VARCHAR).withSize(256).notNull());
        addMetadata(status, ColumnMetadata.named("STATUS").withIndex(4).ofType(Types.SMALLINT).withSize(16).notNull());
        addMetadata(timeStamp, ColumnMetadata.named("TIME_STAMP").withIndex(2).ofType(Types.TIMESTAMP).withSize(26).notNull());
        addMetadata(uri, ColumnMetadata.named("URI").withIndex(3).ofType(Types.VARCHAR).withSize(256));
    }

}

