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
 * QUrlMapping is a Querydsl query type for QUrlMapping
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QUrlMapping extends com.querydsl.sql.RelationalPathBase<QUrlMapping> {

    private static final long serialVersionUID = -871877026;

    public static final QUrlMapping urlMapping = new QUrlMapping("url_mapping");

    public final StringPath destination = createString("destination");

    public final StringPath source = createString("source");

    public QUrlMapping(String variable) {
        super(QUrlMapping.class, forVariable(variable), "PUBLIC", "url_mapping");
        addMetadata();
    }

    public QUrlMapping(String variable, String schema, String table) {
        super(QUrlMapping.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QUrlMapping(String variable, String schema) {
        super(QUrlMapping.class, forVariable(variable), schema, "url_mapping");
        addMetadata();
    }

    public QUrlMapping(Path<? extends QUrlMapping> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "url_mapping");
        addMetadata();
    }

    public QUrlMapping(PathMetadata metadata) {
        super(QUrlMapping.class, metadata, "PUBLIC", "url_mapping");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(destination, ColumnMetadata.named("destination").withIndex(2).ofType(Types.VARCHAR).withSize(256).notNull());
        addMetadata(source, ColumnMetadata.named("source").withIndex(1).ofType(Types.VARCHAR).withSize(256).notNull());
    }

}

