ebms-core implements EbMS 2.0.

ebms-core-2.16.0.jar and up needs jdk 8 and is compiled and tested with openjdk 8
ebms-core version 2.16.0 and up are released in the Central Maven repository:

<dependency>
  <groupId>nl.clockwork.ebms</groupId>
  <artifactId>ebms-core</artifactId>
  <version>2.17.1</version>
</dependency>

ebms adapter for mule and web are no longer released; use ebms-admin instead

For the ebms-admin console see https://sourceforge.net/projects/javaebmsadmin/

================
= Release Notes
================
ebms-core-2.17.1.jar:
- improved logging
- improved error responses
- fixed auto retry responses only if best effort
- fixed configuration issue, that causes send events to be rejected
- fixed server, client and api mode
- added rate limiter

ebms-core-2.17.0.jar:
- added options to enable high availability and horizontal scaling (and throttling)
- added option to use SSL clientCerttificate defined in the CPA to send messages (https.useClientCertificate)
	- added CertificateMapper SOAP service to override defined SSL clientCertificate
- cleaned up and split up SOAP interfaces
- changed SOAP Services:
	- renamed operations from EbMSMessageService:
		- GetMessageIds to GetUnprocessedMessageIds
		- GetMessageEvents to GetUnprocessedMessageEvents
	- removed operations from EbMSMessageService:
		- SendMessageWithAttachments (use SendMessage from ebmsMTOM instead)
		- GetMassageStatus is replaced by GetMessageStatusByMessageId, old GetMessageStatus is removed
		- ProcessMessages (use ProcessMessage instead)
		- ProcessMessageEvents (use ProcessMessageEvent instead)
	- split up CPAService into CPAService and URLMapper
- changed default properties
	- removed properties:
		- ebms.allowMultipleServers (leave property ebms.serverId empty to set allowMultipleServers to false)
		- patch.digipoort.enable (not necessary anymore)
		- patch.oracle.enable (not necessary anymore)
		- patch.cleo.enable (not necessary anymore)
		- cache.disabled (use cache.type instead)
		- eventProcessor.enabled=true (use eventProcessor.type=NONE instead)
	- changed default value of property
		- http.base64Writer to false (writer is disabled anyway because of an issue)
		- https.clientCertificateAuthentication to false
	- added properties:
		- https.useClientCertificate=false
		- client.keystore.keyPassword=${client.keystore.password}
		- client.keystore.defaultAlias=
		- signature.keystore.keyPassword=${signature.keystore.password}
		- encryption.keystore.keyPassword=${encryption.keystore.password}
		- cache.type=DEFAULT (allowed values: DEFAULT(=SPRING) | EHCACHE | IGNITE)
		- eventProcessor.type=DEFAULT (allowed values: NONE | DEFAULT(=DAO) | JMS)
		- deliveryManager.type=DEFAULT (allowed types: DEFAULT(=DAO) | JMS)
		- eventListener.type=DEFAULT (allowed values: DEFAULT(=LOGGING) | DAO | SIMPLE_JMS | JMS | JMS_TEXT)
		- transactionManager.type=DEFAULT (allowed values: DEFAULT | ATOMIKOS)
	* see src/main/resources/nl/clockwork/ebms/default.properties for all available properties
- implemented JMS components (for scaling)
- added Atomikos transaction manager (for JMS)
- added Apache Ignite cache manager (for scaling)
- added Flyway to install and upgrade database 
- code improvements
	- added lombok and vavr
	- made objects immutable where possible
	- moved spring bean configuration from xml to code
	- restructured classes and packages
	- reconfigured caching and transactions
	- split up DAO
	- replaced jdbcTemplate by querydsl
	- replace commons-logging by slf4j
	- lots of other improvements
- updated libraries
- database updates and improved indices

ebms-core-2.16.7.jar:
- disabled base64Writer because of a bug when sending base64 encoded content
- fixed using header defined in property x509CertificateHeader

ebms-core-2.16.6.jar:
- fixed bug: the references in a signed acknowledgment is not validated correctly, which will not set the status of the message to DELIVERED but eventually to EXPIRED instead
- fixed issue using asynchronous messaging and no receive deliveryChannel can be found. The message will be stored and returned synchronously as an error now
- fixed deliveryChannel validation not handled correctly, causing a SOAP fault being returned instead of a EbMS MessageError in case of an error

ebms-core-2.16.5.jar:
- optimized memory usage by using CachedOutputStream for attachments that overflows to disk:
	- added property ebmsMessage.attachment.memoryTreshold - default: 128KB 
	- added property ebmsMessage.attachment.outputDirectory - default: <tempDir>
	- added property ebmsMessage.attachment.cipherTransformation - default: none

ebms-core-2.16.4.jar:
- fixed EbMSEventProcessor: the processor sometimes stops processing after an error occurs, so the ebms adapter stops sending messages
- fixed query in deleteEbMSAttachmentsOnMessageProcessed
- fixed messageId: the hostname is not prepended anymore when the messageId is given
- added new MTOM EbMS soap service

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

==========
= RoapMap
==========
release 2.18.x:
- implement Message Ordering

release 2.19.x:
- upgrade to Java 11

===============
= Introduction
===============
This library contains the core functionality of the EbMS adapter including:
- a servlet to use the adapter in a servlet container
- database support for the following databases:
	- db2
	- h2
	- hsqldb
	- mssql
	- mariadb/mysql
	- oracle
	- postgresql
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

========
= Usage
========
You can use the ebms-core by integrating it into your own java application, or you can use it as a standalone SOAP service through ebms-admin console
 
If you want to use the ebms-core in your own application you can include the jar into your application and configure the adapter through spring properties.
You can include the spring configuration file nl/clockwork/ebms/main.xml into your application or modify it to your needs.
The default spring properties are defined in nl/clockwork/ebms/default.poperties
An application should call the ebms-core through the interfaces defined in nl.clockwork.embs.service.CPAService and nl.clockwork.embs.service.EbMSMessageService
The CPAService contains functionality to control CPAs
The EbMSMessageService contains functionality for sending and receiving ebms messages
To receive ebms messages configure the servlet nl.clockwork.ebms.servlet.EbMSServlet in your application

========================
= EbMS Adapter Database
========================
The EbMS adapter supports different databases:
- DB2
- H2
- HSQLDB
- MariaDB
- MSSQL
- MySQL
- Oracle
- PostgreSQL

The database scripts can be found in https://repo.maven.apache.org/maven2/nl/clockwork/ebms/ebms-core/2.17.0/ebms-core-2.17.0-sources.jar/resources/scripts/database/

You can configure the database by adding the right JDBC driver to the classpath and configuring the right driver and connection string:

# Set username and password
ebms.jdbc.username=<username>
ebms.jdbc.password=<password>

DB2:
ebms.jdbc.driverClassName=com.ibm.db2.jcc.DB2Driver
or
ebms.jdbc.driverClassName=com.ibm.db2.jcc.DB2XADataSource
ebms.jdbc.url=jdbc:db2://<host>:<port>/<dbname>

H2:
ebms.jdbc.driverClassName=org.h2.Driver
or
ebms.jdbc.driverClassName=org.h2.Driver
# In memory
ebms.jdbc.url=jdbc:h2:mem:<dbname>
# or file
ebms.jdbc.url=jdbc:h2:<path>
# or external
ebms.jdbc.url=jdbc:h2:tcp://<host>:<port>/<path>

HSQLDB:
ebms.jdbc.driverClassName=org.hsqldb.jdbcDriver
or
ebms.jdbc.driverClassName=org.hsqldb.jdbc.pool.JDBCXADataSource
# In memory
ebms.jdbc.url=jdbc:hsqldb:mem:<dbname>
# or file
ebms.jdbc.url=jdbc:hsqldb:file:<path>
# or external
ebms.jdbc.url=jdbc:hsqldb:hsql://<host>:<port>/<dbname>

MariaDB:
ebms.jdbc.driverClassName=org.mariadb.jdbc.Driver
or
ebms.jdbc.driverClassName=
ebms.jdbc.url=jdbc:mysql://<host>:<port>/<dbname>

MSSQL:
ebms.jdbc.driverClassName=com.microsoft.sqlserver.jdbc.SQLServerDriver
or
ebms.jdbc.driverClassName=com.microsoft.sqlserver.jdbc.SQLServerXADataSource
ebms.jdbc.url=jdbc:sqlserver://<host>:<port>;[instanceName=<instanceName>;]databaseName=<dbname>;

MySQL:
ebms.jdbc.driverClassName=com.mysql.cj.jdbc.Driver
or
ebms.jdbc.driverClassName=com.mysql.cj.jdbc.MysqlXADataSource
ebms.jdbc.url=jdbc:mysql://<host>:<port>/<dbname>

Oracle:
ebms.jdbc.driverClassName=oracle.jdbc.OracleDriver
or
ebms.jdbc.driverClassName=oracle.jdbc.xa.client.OracleXADataSource
ebms.jdbc.url=jdbc:oracle:thin:@<host>:<port>:<dbname>

PostgreSQL:
ebms.jdbc.driverClassName=org.postgresql.Driver
or
ebms.jdbc.driverClassName=org.postgresql.xa.PGXADataSource
ebms.jdbc.url=jdbc:postgresql://<host>:<port>/<dbname>

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

============
= Resources
============
the resources directory resides in https://repo.maven.apache.org/maven2/nl/clockwork/ebms/ebms-core/2.17.0/ebms-core-2.17.0-sources.jar/resources and contains the following data:
- scripts/database/ - contains the database master scripts for the supported databases
- src/main/resources/nl/clockwork/ebms/db/migration/ - contains the incremental database update scripts for the supported databases

===========
= Building
===========
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

=====================
= Generating reports
=====================
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