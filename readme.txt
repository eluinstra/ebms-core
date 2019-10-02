==============
Introduction =
==============
This library contains the core functionality of the EbMS adapter including:
- a servlet to use the adapter in a servlet container
- database support for the following databases:
	- hsqldb
	- mysql/mariadb
	- postgresql
	- mssql
	- oracle
	- db2
- CPA and EbMSMessage SOAP Services to control the EbMS adapter
- See SourceForge for the EbMS Admin Console.

Implemented:
-	Core Functionality
	-	Security Module
		-	Signature
		-	Encryption
	-	Error Handling Module
	-	SyncReply Module
-	Additional Features:
	-	Reliable Messaging Module
	-	Message Status Service
	-	Message Service Handler Ping Service
-	HTTP(S) Protocol
-	Separate ErrorMessage and Acknowledgment messages

Not implemented:
-	Core Functionality
	o	Packaging
-	Additional Features:
	o	Message Order Module
	o	Multi-Hop Module
-	Multiple delivery channels per action

Remarks:
-	Duplicate messages will always be eliminated
-	Only standalone MSH level messages are supported.
-	Only acts as ToPartyMSH, not as nextMSH
-	Only 1 (allPurpose) Channel per Action is supported
-	Manifest can only refer to payload data included as part of the message as payload document(s) contained in a Payload Container
-	Extendible to support other communication protocols

=======
Usage =
=======
You can use the ebms-core by integrating it into your own java application, or you can use it as a standalone SOAP service through one of the application wrappers (ebms-admin, ebms-adapter-web or ebms-adapter-mule)
 
If you want to use the ebms-core in your own application you can include the jar into your application and configure the adapter through spring properties.
You can include the spring configuration file nl/clockwork/ebms/main.xml into your application or modify it to your needs.
The default spring properties are defined in nl/clockwork/ebms/default.poperties
An application should call the ebms-core through the interfaces defined in nl.clockwork.embs.service.CPAService and nl.clockwork.embs.service.EbMSMessageService
The CPAService contains functionality to control CPAs
The EbMSMessageService contains functionality for sending and receiving ebms messages
To receive ebms messages configure the servlet nl.clockwork.ebms.servlet.EbMSServlet in your application

=======================
EbMS Adapter Database =
=======================
The EbMS adapter supports different databases:
- HSQLDB
- MySQL
- MariaDB
- PostgreSQL
- MSSQL
- Oracle
- DB2

You can configure them by configuring the right driver and connection string:

# Set username and password
ebms.jdbc.username=<username>
ebms.jdbc.password=<password>

HSQLDB:
ebms.jdbc.driverClassName=org.hsqldb.jdbcDriver
# In memory
ebms.jdbc.url=jdbc:hsqldb:mem:<dbname>
# or file
ebms.jdbc.url=jdbc:hsqldb:file:<path>
# or external
ebms.jdbc.url=jdbc:hsqldb:hsql://<host>:<port>/<dbname>
ebms.pool.preferredTestQuery=select 1 from information_schema.system_tables

MySQL:
ebms.jdbc.driverClassName=com.mysql.jdbc.Driver
ebms.jdbc.url=jdbc:mysql://<host>:<port>/<dbname>
ebms.pool.preferredTestQuery=select 1

MariaDB:
ebms.jdbc.driverClassName=org.mariadb.jdbc.Driver
ebms.jdbc.url=jdbc:mysql://localhost:3306/ebms
ebms.pool.preferredTestQuery=select 1

PostgreSQL:
ebms.jdbc.driverClassName=org.postgresql.Driver
ebms.jdbc.url=jdbc:postgresql://<host>:<port>/<dbname>
ebms.pool.preferredTestQuery=select 1

MSSQL:
ebms.jdbc.driverClassName=com.microsoft.sqlserver.jdbc.SQLServerDriver
ebms.jdbc.url=jdbc:sqlserver://<host>:<port>;databaseName=<dbname>;
ebms.pool.preferredTestQuery=select 1

Oracle:
ebms.jdbc.driverClassName=oracle.jdbc.OracleDriver
ebms.jdbc.url=jdbc:oracle:thin:@<host>:<port>:<dbname>

DB2:
ebms.jdbc.driverClassName=com.ibm.db2.jcc.DB2Driver
ebms.jdbc.url=jdbc:db2://localhost:50000/ebms
ebms.pool.preferredTestQuery=select 1 from sysibm.sysdummy1

If you want to let the adapter use the application datasource exclude the following file:
- nl/clockwork/ebms/datasource.xml
and add the name ebMSDataSource to the application datasource 

==========
Security =
==========
SSL:
keystore.path=keystore.jks
keystore.password=password

truststore.path=truststore.jks
truststore.password=password

-Dhttps.protocols="TLSv1.2"
-Dhttps.cipherSuites="TLS_RSA_WITH_AES_256_CBC_SHA256","TLS_RSA_WITH_AES_128_CBC_SHA256","TLS_RSA_WITH_AES_256_CBC_SHA","TLS_RSA_WITH_AES_128_CBC_SHA"

Signing:
signature.keystore.path=keystore.jks
signature.keystore.password=password

Encryption:
encryption.keystore.path=keystore.jks
encryption.keystore.password=password

===========
Resources =
===========
the reources directory resides in ebms-adapter-x.x.x.zip/resources and contains the following data:
- scripts/database/ - contains the database scripts for the supported databases

==========
Building =
==========
The Maven settings.xml requires additional settings to support the Oracle Maven Repository. Add the following <server> element to the <servers> section of the Maven settings.xml:

 <server>
    <id>maven.oracle.com</id>
    <username>username</username>
    <password>password</password>
    <configuration>
      <basicAuthScope>
        <host>ANY</host>
        <port>ANY</port>
        <realm>OAM 11g</realm>
      </basicAuthScope>
      <httpConfiguration>
        <all>
          <params>
            <property>
              <name>http.protocol.allow-circular-redirects</name>
              <value>%b,true</value>
            </property>
          </params>
        </all>
      </httpConfiguration>
    </configuration>
  </server>

Replace the <username> and <password> entries with your OTN user name and password.

mvn package

====================
Generating reports =
====================
mvn site

Or to generate individual reports:
mvn surefire:test
mvn jxr:jxr
mvn jxr:test-jxr
mvn checkstyle:checkstyle
mvn findbugs:findbugs
mvn pmd:pmd
mvn jdepend:generate
mvn cobertura:cobertura
mvn org.owasp:dependency-check-maven:5.1.0:check