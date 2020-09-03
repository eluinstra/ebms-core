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
package nl.clockwork.ebms.delivery.task;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QDeliveryLog is a Querydsl query type for QDeliveryLog
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QDeliveryLog extends com.querydsl.sql.RelationalPathBase<QDeliveryLog> {

    private static final long serialVersionUID = -433552212;

    public static final QDeliveryLog deliveryLog = new QDeliveryLog("delivery_log");

    public final StringPath errorMessage = createString("errorMessage");

    public final StringPath messageId = createString("messageId");

    public final EnumPath<nl.clockwork.ebms.delivery.task.DeliveryTaskStatus> status = createEnum("status", nl.clockwork.ebms.delivery.task.DeliveryTaskStatus.class);

    public final DateTimePath<java.time.Instant> timeStamp = createDateTime("timeStamp", java.time.Instant.class);

    public final StringPath uri = createString("uri");

    public QDeliveryLog(String variable) {
        super(QDeliveryLog.class, forVariable(variable), "PUBLIC", "delivery_log");
        addMetadata();
    }

    public QDeliveryLog(String variable, String schema, String table) {
        super(QDeliveryLog.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QDeliveryLog(String variable, String schema) {
        super(QDeliveryLog.class, forVariable(variable), schema, "delivery_log");
        addMetadata();
    }

    public QDeliveryLog(Path<? extends QDeliveryLog> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "delivery_log");
        addMetadata();
    }

    public QDeliveryLog(PathMetadata metadata) {
        super(QDeliveryLog.class, metadata, "PUBLIC", "delivery_log");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(errorMessage, ColumnMetadata.named("error_message").withIndex(5).ofType(Types.CLOB).withSize(1073741824));
        addMetadata(messageId, ColumnMetadata.named("message_id").withIndex(1).ofType(Types.VARCHAR).withSize(256).notNull());
        addMetadata(status, ColumnMetadata.named("status").withIndex(4).ofType(Types.SMALLINT).withSize(16).notNull());
        addMetadata(timeStamp, ColumnMetadata.named("time_stamp").withIndex(2).ofType(Types.TIMESTAMP).withSize(26).notNull());
        addMetadata(uri, ColumnMetadata.named("uri").withIndex(3).ofType(Types.VARCHAR).withSize(256));
    }

}

