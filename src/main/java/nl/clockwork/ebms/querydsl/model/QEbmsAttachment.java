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
 * QEbmsAttachment is a Querydsl query type for QEbmsAttachment
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QEbmsAttachment extends com.querydsl.sql.RelationalPathBase<QEbmsAttachment> {

    private static final long serialVersionUID = 1332957957;

    public static final QEbmsAttachment ebmsAttachment = new QEbmsAttachment("ebms_attachment");

    public final SimplePath<org.apache.cxf.io.CachedOutputStream> content = createSimple("content", org.apache.cxf.io.CachedOutputStream.class);

    public final StringPath contentId = createString("contentId");

    public final StringPath contentType = createString("contentType");

    public final StringPath messageId = createString("messageId");

    public final NumberPath<Integer> messageNr = createNumber("messageNr", Integer.class);

    public final StringPath name = createString("name");

    public final NumberPath<Short> orderNr = createNumber("orderNr", Short.class);

    public final com.querydsl.sql.ForeignKey<QEbmsMessage> sysFk10141 = createForeignKey(Arrays.asList(messageId, messageNr), Arrays.asList("message_id", "message_nr"));

    public QEbmsAttachment(String variable) {
        super(QEbmsAttachment.class, forVariable(variable), "PUBLIC", "ebms_attachment");
        addMetadata();
    }

    public QEbmsAttachment(String variable, String schema, String table) {
        super(QEbmsAttachment.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QEbmsAttachment(String variable, String schema) {
        super(QEbmsAttachment.class, forVariable(variable), schema, "ebms_attachment");
        addMetadata();
    }

    public QEbmsAttachment(Path<? extends QEbmsAttachment> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "ebms_attachment");
        addMetadata();
    }

    public QEbmsAttachment(PathMetadata metadata) {
        super(QEbmsAttachment.class, metadata, "PUBLIC", "ebms_attachment");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(content, ColumnMetadata.named("content").withIndex(7).ofType(Types.BLOB).withSize(1073741824).notNull());
        addMetadata(contentId, ColumnMetadata.named("content_id").withIndex(5).ofType(Types.VARCHAR).withSize(256).notNull());
        addMetadata(contentType, ColumnMetadata.named("content_type").withIndex(6).ofType(Types.VARCHAR).withSize(255).notNull());
        addMetadata(messageId, ColumnMetadata.named("message_id").withIndex(1).ofType(Types.VARCHAR).withSize(256).notNull());
        addMetadata(messageNr, ColumnMetadata.named("message_nr").withIndex(2).ofType(Types.SMALLINT).withSize(16).notNull());
        addMetadata(name, ColumnMetadata.named("name").withIndex(4).ofType(Types.VARCHAR).withSize(256));
        addMetadata(orderNr, ColumnMetadata.named("order_nr").withIndex(3).ofType(Types.SMALLINT).withSize(16).notNull());
    }

}

