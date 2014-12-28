==============
Introduction =
==============
This library contains the core functionality of the EbMS adapter including:
- a servlet to use the adapter in a servlet container
- database support for the following databases:
	- hsqldb (2.2.9)
	- mysql (5.5) (innodb)
	- postgresql (9)
	- mssql (2008 R2)
	- oracle (11)(lob > 4k fix (needed for older oracle jdbc adapters) not implemented!)
- CPA and EbMSMessage SOAP Services to control the EbMS adapter

Supported:
-	Core Functionality
	o	Security Module
			Signature
	o	Error Handling Module
	o	SyncReply Module
-	Additional Features:
	o	Reliable Messaging Module
	o	Message Status Service
	o	Message Service Handler Ping Service
-	HTTP(S) Protocol
-	Separate ErrorMessage and Acknowlegment messages

Not supported:
-	Core Functionality
	o	Security Module
			Encryption
-	Additional Features:
	o	Message Order Module
	o	Multi-Hop Module
-	Multiple delivery channels per action
-	Manifest inspection

Remarks:
-	Duplicate messages will always be eliminated
-	Extendable to support other communication protocols

===============
Prerequisites =
===============
download and copy the following libraries:
- c3p0-0.9.1.2.jar
- depending on the database used:
	- hsqldb-2.2.9.jar
	- mysql-connector-java-5.1.18.jar
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
- PostgreSQL
- MSSQL
- Oracle

You can configure them by including the right xml in your project:
- nl/clockwork/ebms/components/hsqldb.xml
- nl/clockwork/ebms/components/mysql.xml
- nl/clockwork/ebms/components/postgresql.xml
- nl/clockwork/ebms/components/mssql.xml
- nl/clockwork/ebms/components/oracle.xml

And you have to configure the right driver and connection string:
- ebms.jdbc.driverClassName=org.hsqldb.jdbcDriver
	ebms.jdbc.url=jdbc:hsqldb:mem:<dbname>
	or
	ebms.jdbc.url=jdbc:hsqldb:file:<path>
	or
	ebms.jdbc.url=jdbc:hsqldb:hsql://<host>:<port>/<dbname>

- ebms.jdbc.driverClassName=com.mysql.jdbc.Driver
	ebms.jdbc.url=jdbc:mysql://<host>:<port>/<dbname>

- ebms.jdbc.driverClassName=org.postgresql.Driver
	ebms.jdbc.url=jdbc:postgresql://<host>:<port>/<dbname>

- ebms.jdbc.driverClassName=com.microsoft.sqlserver.jdbc.SQLServerDriver
	or
	ebms.jdbc.driverClassName=net.sourceforge.jtds.jdbc.Driver
	ebms.jdbc.url=jdbc:sqlserver://<host>:<port>;databaseName=<dbname>;

- ebms.jdbc.driverClassName=oracle.jdbc.OracleDriver
	ebms.jdbc.url=jdbc:oracle:thin:@<host>:<port>:<dbname>

If you want to let the adapter use the application datasource exclude the following file:
- nl/clockwork/ebms/components/datasource.xml
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

===============
Functionality =
===============

- Encoding is not supported
- Message Order is not supported
- Multi-Hop is not supported
	- Cannot act as intermediary MSH
		- Actor ToPartyMSH is supported
		- Actor NextMSH is not supported

- Always eliminates duplicate messages
	- messageId is globally unique
	- all messages are stored
- Manifest can only refer to payload data included as part of the message as payload document(s) contained in a Payload Container, not to remote resources accessible via a URL
- SOAP Fault messages can be generated
- Only 1 Channel per Action is supported
- ErrorList and Acknowledgment elements as part of another message are not supported.
  Only error messages with a MessageHeader containing service 'urn:oasis:names:tc:ebxml-msg:service' and action 'MessageError' are supported
  Only aknowledgment messages with a MessageHeader containing service 'urn:oasis:names:tc:ebxml-msg:service' and action 'Acknowledgment' are supported

- Only one transport is supported

- ErrorMessages
	- only custom, StatusRequest and Ping messages can receive an ErrorMessage
	- ErrorMessage, Acknowledgment, StatusRequest, StatusResponse, Ping and Pong messages can never receive an ErrorMessage
- Acknowledgments
	- only custom messages can receive an Acknowledgment if messaging is reliable
	- ErrorMessage, Acknowledgment, StatusRequest, StatusResponse, Ping, Pong messages can never receive an Acknowledgment
- if duplicate message is received, the response, if any, (ErrorMessage, Aknowledgment(, StatusResponse, Pong)) will be resend
- ackRequested(, ackSignatureRequested) and duplicateElimination are ignored on the DefaultMSHChannel
