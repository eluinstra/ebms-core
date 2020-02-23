ebms-core implements EbMS 2.0.

ebms-core-2.16.0.jar and up needs jdk 8 and is compiled and tested with openjdk 8
ebms-core version 2.16.0 and up are released in the Central Maven repository:

<dependency>
  <groupId>nl.clockwork.ebms</groupId>
  <artifactId>ebms-core</artifactId>
  <version>2.16.3</version>
</dependency>

ebms adapter for mule and web are no longer released; use ebms-admin instead

For the ebms-admin console see https://sourceforge.net/projects/javaebmsadmin/

===============
= Release Notes
===============
ebms-core-2.16.3.jar:
- fixed bug: messages are sometimes sent more than once at (almost) the same time
- improved EbMSEventProcessor
- renamed property jobScheduler.enabled to eventProcessor.enabled
- renamed property jobScheduler.delay to eventProcessor.delay
- renamed property jobScheduler.period to eventProcessor.period
- renamed property job.maxTreads to eventProcessor.maxTreads
- renamed property job.processorsScaleFactor to eventProcessor.processorsScaleFactor
- renamed property job.queueScaleFactor to eventProcessor.queueScaleFactor
- improved EbMSResponseHandler
- renamed property http.errors.server.irrecoverable to http.errors.server.unrecoverable

ebms-core-2.16.2.jar:
- removed MIME-Version header

ebms-core-2.16.1.jar:
- improved client ssl behaviour
- added keystore type support
- minor improvements

ebms-core-2.16.0.jar:
- upgraded to java 8
- minor improvements

==============
= Introduction
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

Not implemented:
-	Core Functionality
	o	Packaging
-	Additional Features:
	o	Message Order Module
	o	Multi-Hop Module

Remarks:
-	Duplicate messages will always be eliminated
-	Only standalone MSH level messages are supported
-	Only acts as ToPartyMSH, not as nextMSH
-	Only 1 (=allPurpose) Channel per Action is supported
-	Manifest can only refer to payload data included as part of the message
-	Extendible to support other communication protocols

=======
= Usage
=======
You can use the ebms-core by integrating it into your own java application, or you can use it as a standalone SOAP service through ebms-admin console
 
If you want to use the ebms-core in your own application you can include the jar into your application and configure the adapter through spring properties.
You can include the spring configuration file nl/clockwork/ebms/main.xml into your application or modify it to your needs.
The default spring properties are defined in nl/clockwork/ebms/default.poperties
An application should call the ebms-core through the interfaces defined in nl.clockwork.embs.service.CPAService and nl.clockwork.embs.service.EbMSMessageService
The CPAService contains functionality to control CPAs
The EbMSMessageService contains functionality for sending and receiving ebms messages
To receive ebms messages configure the servlet nl.clockwork.ebms.servlet.EbMSServlet in your application

=======================
= EbMS Adapter Database
=======================
The EbMS adapter supports different databases:
- HSQLDB
- MySQL
- MariaDB
- PostgreSQL
- MSSQL
- Oracle
- DB2

You can configure the database by adding the right JDBC driver to the classpath and configuring the right driver and connection string:

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

==========
= Security
==========
# SSL truststore:
truststore.type=PKCS12
truststore.path=truststore.p12
truststore.password=password

# SSL client keystore:
client.keystore.type=PKCS12
client.keystore.path=keystore.p12
client.keystore.password=password

-Dhttps.protocols="TLSv1.2"
-Dhttps.cipherSuites="TLS_RSA_WITH_AES_256_CBC_SHA256","TLS_RSA_WITH_AES_128_CBC_SHA256","TLS_RSA_WITH_AES_256_CBC_SHA","TLS_RSA_WITH_AES_128_CBC_SHA"

# EbMS Signing keystore
signature.keystore.type=PKCS12
signature.keystore.path=keystore.p12
signature.keystore.password=password

# EbMS Encryption keystore
encryption.keystore.type=PKCS12
encryption.keystore.path=keystore.p12
encryption.keystore.password=password

===========
= Resources
===========
the reources directory resides in ebms-core-x.x.x-src.zip/resources and contains the following data:
- scripts/database/ - contains the database scripts for the supported databases

==========
= Building
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
= Generating reports
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