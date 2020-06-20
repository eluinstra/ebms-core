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

    public static final QEbmsMessageEvent ebmsMessageEvent = new QEbmsMessageEvent("EBMS_MESSAGE_EVENT");

    public final EnumPath<nl.clockwork.ebms.event.listener.EbMSMessageEventType> eventType = createEnum("eventType", nl.clockwork.ebms.event.listener.EbMSMessageEventType.class);

    public final StringPath messageId = createString("messageId");

    public final BooleanPath processed = createBoolean("processed");

    public final DateTimePath<java.sql.Timestamp> timeStamp = createDateTime("timeStamp", java.sql.Timestamp.class);

    public QEbmsMessageEvent(String variable) {
        super(QEbmsMessageEvent.class, forVariable(variable), "PUBLIC", "EBMS_MESSAGE_EVENT");
        addMetadata();
    }

    public QEbmsMessageEvent(String variable, String schema, String table) {
        super(QEbmsMessageEvent.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QEbmsMessageEvent(String variable, String schema) {
        super(QEbmsMessageEvent.class, forVariable(variable), schema, "EBMS_MESSAGE_EVENT");
        addMetadata();
    }

    public QEbmsMessageEvent(Path<? extends QEbmsMessageEvent> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "EBMS_MESSAGE_EVENT");
        addMetadata();
    }

    public QEbmsMessageEvent(PathMetadata metadata) {
        super(QEbmsMessageEvent.class, metadata, "PUBLIC", "EBMS_MESSAGE_EVENT");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(eventType, ColumnMetadata.named("EVENT_TYPE").withIndex(2).ofType(Types.SMALLINT).withSize(16).notNull());
        addMetadata(messageId, ColumnMetadata.named("MESSAGE_ID").withIndex(1).ofType(Types.VARCHAR).withSize(256).notNull());
        addMetadata(processed, ColumnMetadata.named("PROCESSED").withIndex(4).ofType(Types.SMALLINT).withSize(16).notNull());
        addMetadata(timeStamp, ColumnMetadata.named("TIME_STAMP").withIndex(3).ofType(Types.TIMESTAMP).withSize(26).notNull());
    }

}

