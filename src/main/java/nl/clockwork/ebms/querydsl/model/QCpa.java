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
 * QCpa is a Querydsl query type for QCpa
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QCpa extends com.querydsl.sql.RelationalPathBase<QCpa> {

    private static final long serialVersionUID = 1655323765;

    public static final QCpa cpa1 = new QCpa("cpa");

    public final SimplePath<org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement> cpa =
    		createSimple("cpa", org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement.class);

    public final StringPath cpaId = createString("cpaId");

    public QCpa(String variable) {
        super(QCpa.class, forVariable(variable), "PUBLIC", "cpa");
        addMetadata();
    }

    public QCpa(String variable, String schema, String table) {
        super(QCpa.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QCpa(String variable, String schema) {
        super(QCpa.class, forVariable(variable), schema, "cpa");
        addMetadata();
    }

    public QCpa(Path<? extends QCpa> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "cpa");
        addMetadata();
    }

    public QCpa(PathMetadata metadata) {
        super(QCpa.class, metadata, "PUBLIC", "cpa");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(cpa, ColumnMetadata.named("cpa").withIndex(2).ofType(Types.CLOB).withSize(1073741824).notNull());
        addMetadata(cpaId, ColumnMetadata.named("cpa_id").withIndex(1).ofType(Types.VARCHAR).withSize(256).notNull());
    }

}

