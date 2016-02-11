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

===============
Prerequisites =
===============
download and copy the following libraries:
- c3p0-0.9.1.2.jar
- depending on the database used:
	- hsqldb-2.2.9.jar
	- mysql-connector-java-5.1.18.jar
	- mariadb-java-client-1.1.9.jar
	- postgresql-9.1-901.jdbc3.jar or postgresql-9.1-901.jdbc4.jar
	- jtds-1.2.4.jar or sqljdbc4-201004.jar
	- ojdbc6-11.2.0.1.0.jar (and orai18n-11.2.0.1.0.jar)

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
	or
	ebms.jdbc.driverClassName=net.sourceforge.jtds.jdbc.Driver
	ebms.jdbc.url=jdbc:sqlserver://<host>:<port>;databaseName=<dbname>;

- ebms.jdbc.driverClassName=oracle.jdbc.OracleDriver
	ebms.jdbc.url=jdbc:oracle:thin:@<host>:<port>:<dbname>

If you want to let the adapter use the application datasource exclude the following file:
- nl/clockwork/ebms/datasource.xml
and add the name ebMSDataSource to the application datasource 

===========
Resources =
===========
the reources directory resides in ebms-adapter-x.x.x.zip/resources and contains the following data:
	scripts/database/ - contains the database scripts for the supported databases

==========
Building =
==========
> mvn package

====================
Generating reports =
====================
> mvn site

Or to generate individual reports:
> mvn jxr:jxr
> mvn jxr:test-jxr
> mvn checkstyle:checkstyle
> mvn findbugs:findbugs
> mvn pmd:pmd
> mvn jdepend:generate
> mvn cobertura:cobertura
