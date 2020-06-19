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
 * QEbmsAttachment is a Querydsl query type for QEbmsAttachment
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QEbmsAttachment extends com.querydsl.sql.RelationalPathBase<QEbmsAttachment> {

    private static final long serialVersionUID = 1332957957;

    public static final QEbmsAttachment ebmsAttachment = new QEbmsAttachment("EBMS_ATTACHMENT");

    public final SimplePath<java.sql.Blob> content = createSimple("content", java.sql.Blob.class);

    public final StringPath contentId = createString("contentId");

    public final StringPath contentType = createString("contentType");

    public final StringPath messageId = createString("messageId");

    public final NumberPath<Short> messageNr = createNumber("messageNr", Short.class);

    public final StringPath name = createString("name");

    public final NumberPath<Short> orderNr = createNumber("orderNr", Short.class);

    public final com.querydsl.sql.ForeignKey<QEbmsMessage> sysFk10141 = createForeignKey(Arrays.asList(messageId, messageNr), Arrays.asList("MESSAGE_ID", "MESSAGE_NR"));

    public QEbmsAttachment(String variable) {
        super(QEbmsAttachment.class, forVariable(variable), "PUBLIC", "EBMS_ATTACHMENT");
        addMetadata();
    }

    public QEbmsAttachment(String variable, String schema, String table) {
        super(QEbmsAttachment.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QEbmsAttachment(String variable, String schema) {
        super(QEbmsAttachment.class, forVariable(variable), schema, "EBMS_ATTACHMENT");
        addMetadata();
    }

    public QEbmsAttachment(Path<? extends QEbmsAttachment> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "EBMS_ATTACHMENT");
        addMetadata();
    }

    public QEbmsAttachment(PathMetadata metadata) {
        super(QEbmsAttachment.class, metadata, "PUBLIC", "EBMS_ATTACHMENT");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(content, ColumnMetadata.named("CONTENT").withIndex(7).ofType(Types.BLOB).withSize(1073741824).notNull());
        addMetadata(contentId, ColumnMetadata.named("CONTENT_ID").withIndex(5).ofType(Types.VARCHAR).withSize(256).notNull());
        addMetadata(contentType, ColumnMetadata.named("CONTENT_TYPE").withIndex(6).ofType(Types.VARCHAR).withSize(255).notNull());
        addMetadata(messageId, ColumnMetadata.named("MESSAGE_ID").withIndex(1).ofType(Types.VARCHAR).withSize(256).notNull());
        addMetadata(messageNr, ColumnMetadata.named("MESSAGE_NR").withIndex(2).ofType(Types.SMALLINT).withSize(16).notNull());
        addMetadata(name, ColumnMetadata.named("NAME").withIndex(4).ofType(Types.VARCHAR).withSize(256));
        addMetadata(orderNr, ColumnMetadata.named("ORDER_NR").withIndex(3).ofType(Types.SMALLINT).withSize(16).notNull());
    }

}

