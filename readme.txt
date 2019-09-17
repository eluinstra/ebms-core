==============
Introduction =
==============
This library contains the core functionality of the EbMS adapter including:
- a servlet to use the adapter in a servlet container
- database support for the following databases:
	- hsqldb (>=2.2.9)
	- mysql/mariadb (>=5.5)
	- postgresql (>=9)
	- mssql (>=2008 R2)
	- oracle (>=11)
- CPA and EbMSMessage SOAP Services to control the EbMS adapter
- See SourceForge for the EbMS Admin Console.

Implemented:
-	Core Functionality
	o	Security Module
			Signature
			Encryption
	o	Error Handling Module
	o	SyncReply Module
-	Additional Features:
	o	Reliable Messaging Module
	o	Message Status Service
	o	Message Service Handler Ping Service
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
You can use the ebms-adapter in your own application you can include this project in your project and configure the adapter into your application.
Use nl/clockwork/ebms/main.xml as a starting point.
You will have to generate your own CPAs and application flow.

At the defined endpoint the application will receive an object of type EbMSMessageContent that contains:
- EbMSMessageContext (needed to reply on this message)
- properties (contain the properties from the EbMS Header defined in application property ebms.message.header.properties
- attachments (the actual EbMS Message content)

The application can instantiate a new message or reply to a received message by calling the EbMSMessageService.
The application should wrap the content of the message in an object of type EbMSMessageContent as attachments.
If the message is a response to a previous received message, then include the EbMSMessageContext of the previous message.
The EbMS adapter will then correlate these two messages.
If the message is a new message, then leave the EbMSMessageContext empty.

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

You can configure them by configuring the right driver and connection string:
- ebms.jdbc.driverClassName=org.hsqldb.jdbcDriver
	ebms.jdbc.url=jdbc:hsqldb:mem:<dbname>
	or
	ebms.jdbc.url=jdbc:hsqldb:file:<path>
	or
	ebms.jdbc.url=jdbc:hsqldb:hsql://<host>:<port>/<dbname>

- ebms.jdbc.driverClassName=com.mysql.jdbc.Driver
	ebms.jdbc.url=jdbc:mysql://<host>:<port>/<dbname>

- ebms.jdbc.driverClassName=org.mariadb.jdbc.Driver
  ebms.jdbc.url=jdbc:mysql://localhost:3306/ebms

- ebms.jdbc.driverClassName=org.postgresql.Driver
	ebms.jdbc.url=jdbc:postgresql://<host>:<port>/<dbname>

- ebms.jdbc.driverClassName=com.microsoft.sqlserver.jdbc.SQLServerDriver
	ebms.jdbc.url=jdbc:sqlserver://<host>:<port>;databaseName=<dbname>;

- ebms.jdbc.driverClassName=oracle.jdbc.OracleDriver
	ebms.jdbc.url=jdbc:oracle:thin:@<host>:<port>:<dbname>

If you want to let the adapter use the application datasource exclude the following file:
- nl/clockwork/ebms/datasource.xml
and add the name ebMSDataSource to the application datasource 

==========
Security =
==========
SSL:
keystore.path=keystore.jks
keystore.password=password

truststore.path=keystore.jks
truststore.password=password

-Dhttps.protocols="TLSv1.2","TLSv1.1","TLSv1"
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
	scripts/database/ - contains the database scripts for the supported databases

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