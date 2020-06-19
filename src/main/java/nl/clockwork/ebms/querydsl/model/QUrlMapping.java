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

    public static final QUrlMapping urlMapping = new QUrlMapping("URL_MAPPING");

    public final StringPath destination = createString("destination");

    public final StringPath source = createString("source");

    public QUrlMapping(String variable) {
        super(QUrlMapping.class, forVariable(variable), "PUBLIC", "URL_MAPPING");
        addMetadata();
    }

    public QUrlMapping(String variable, String schema, String table) {
        super(QUrlMapping.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QUrlMapping(String variable, String schema) {
        super(QUrlMapping.class, forVariable(variable), schema, "URL_MAPPING");
        addMetadata();
    }

    public QUrlMapping(Path<? extends QUrlMapping> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "URL_MAPPING");
        addMetadata();
    }

    public QUrlMapping(PathMetadata metadata) {
        super(QUrlMapping.class, metadata, "PUBLIC", "URL_MAPPING");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(destination, ColumnMetadata.named("DESTINATION").withIndex(2).ofType(Types.VARCHAR).withSize(256).notNull());
        addMetadata(source, ColumnMetadata.named("SOURCE").withIndex(1).ofType(Types.VARCHAR).withSize(256).notNull());
    }

}

