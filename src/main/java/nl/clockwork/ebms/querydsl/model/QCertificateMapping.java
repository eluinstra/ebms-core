package nl.clockwork.ebms.querydsl.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QCertificateMapping is a Querydsl query type for QCertificateMapping
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QCertificateMapping extends com.querydsl.sql.RelationalPathBase<QCertificateMapping> {

    private static final long serialVersionUID = -287584714;

    public static final QCertificateMapping certificateMapping = new QCertificateMapping("CERTIFICATE_MAPPING");

    public final StringPath cpaId = createString("cpaId");

    public final SimplePath<java.security.cert.X509Certificate> destination = createSimple("destination", java.security.cert.X509Certificate.class);

    public final StringPath id = createString("id");

    public final SimplePath<java.security.cert.X509Certificate> source = createSimple("source", java.security.cert.X509Certificate.class);

    public QCertificateMapping(String variable) {
        super(QCertificateMapping.class, forVariable(variable), "PUBLIC", "CERTIFICATE_MAPPING");
        addMetadata();
    }

    public QCertificateMapping(String variable, String schema, String table) {
        super(QCertificateMapping.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QCertificateMapping(String variable, String schema) {
        super(QCertificateMapping.class, forVariable(variable), schema, "CERTIFICATE_MAPPING");
        addMetadata();
    }

    public QCertificateMapping(Path<? extends QCertificateMapping> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "CERTIFICATE_MAPPING");
        addMetadata();
    }

    public QCertificateMapping(PathMetadata metadata) {
        super(QCertificateMapping.class, metadata, "PUBLIC", "CERTIFICATE_MAPPING");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(cpaId, ColumnMetadata.named("CPA_ID").withIndex(4).ofType(Types.VARCHAR).withSize(256));
        addMetadata(destination, ColumnMetadata.named("DESTINATION").withIndex(3).ofType(Types.BLOB).withSize(1073741824).notNull());
        addMetadata(id, ColumnMetadata.named("ID").withIndex(1).ofType(Types.VARCHAR).withSize(256).notNull());
        addMetadata(source, ColumnMetadata.named("SOURCE").withIndex(2).ofType(Types.BLOB).withSize(1073741824).notNull());
    }

}

