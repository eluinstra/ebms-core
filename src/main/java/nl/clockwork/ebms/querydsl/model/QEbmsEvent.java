package nl.clockwork.ebms.querydsl.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




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

    public final NumberPath<Short> retries = createNumber("retries", Short.class);

    public final StringPath sendChannelId = createString("sendChannelId");

    public final StringPath serverId = createString("serverId");

    public final DateTimePath<java.sql.Timestamp> timeStamp = createDateTime("timeStamp", java.sql.Timestamp.class);

    public final DateTimePath<java.sql.Timestamp> timeToLive = createDateTime("timeToLive", java.sql.Timestamp.class);

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

