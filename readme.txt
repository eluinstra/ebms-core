==============
Introduction =
==============

This version will support the full Digikoppeling EbMS Deployment Profile (see Koppelvlakstandaard ebMS Voor Digikoppeling 2.0 Versie 2.2) which is a subset of the ebXML Message Service 2.0 specification by OASIS.

This ebms-adapter runs on mule-standalone-2.2.1. You can use the ebms-adapter in two ways:
- include it in your application/project (only possible if your project also runs on mule-standalone-2.2.1)
- run it standalone and use the tcp bridge to connect to your application

This project includes 2 stubs (digipoort en overheid) that implement the AfleverService and the AanleverService. These stubs can communicate with each other.
The project also includes a standalone ebms-adapter configuration that has a tcp bridge to communicate with an application.
The project includes a standalone version of the overheid stub that uses the standalone ebms-adapter (through tcp), that can also communicate with the digipoort stub.
You can use the overheid (standalone) stub as a starting point for your project.

===============
Prerequisites =
===============
install mule-standalone-2.2.1
build or download ebms-adapter-1.0.0.jar
move the ebms-adapter-1.0.0.jar to <mule-standalone-2.2.1>/lib/user

download and copy the following libraries to <mule-standalone-2.2.1>/lib/opt:
- c3p0-0.9.1.2.jar
- depending on the database used:
	- hsqldb-2.1.0.jar
	- mysql-connector-java-5.1.18.jar
	- postgresql-9.1-901.jdbc3.jar or postgresql-9.1-901.jdbc4.jar
	- jtds-1.2.4.jar or sqljdbc4-201004.jar
	- ojdbc6-11.2.0.1.0.jar (and orai18n-11.2.0.1.0.jar)

supported databases:
- hsqldb 2.1.0
- mysql (5.5) (innodb)
- postgresql (9)
- mssql 2008 R2
- oracle (11)(lob > 4k fix (needed for older oracle versions) not implemented!)

=================
Configure Stubs =
=================
create directory ${user.home}/.ebms-stub

copy ebms-adapter-1.0.0.jar/keystore.jks to ${user.home}/.ebms-stub

	============================
	Configure digipoort EbMS stub =
	============================
	create file ${user.home}/.ebms-stub/ebf.digipoort.properties (this overrides properties from nl/clockwork/mule/ebms/default.properties and nl/clockwork/mule/ebms/stub/ebf/default.digipoort.properties)
	
	edit and add the following lines to ${user.home}/.ebms-stub/ebf.digipoort.properties:
	
	service.port=4443
	service.remote.port=443
	
	server.keystore.path=${user.home}/.ebms-stub/keystore.jks
	client.keystore.path=${user.home}/.ebms-stub/keystore.jks
	truststore.path=${user.home}/.ebms-stub/keystore.jks
	signature.keystore.path=${user.home}/.ebms-stub/keystore.jks
	
	dir.base=H:/tmp/ebms-stub/digipoort
	
	jmx.port=1099
	
	===========================
	Configure overheid EbMS stub =
	===========================
	create file ${user.home}/.ebms-stub/ebf.overheid.properties (this overrides properties from nl/clockwork/mule/ebms/default.properties and nl/clockwork/mule/ebms/stub/ebf/default.overheid.properties)
	
	edit and add the following lines to ${user.home}/.ebms-stub/ebf.overheid.properties:
	
	service.port=443
	service.remote.port=4443
	
	server.keystore.path=${user.home}/.ebms-stub/keystore.jks
	client.keystore.path=${user.home}/.ebms-stub/keystore.jks
	truststore.path=${user.home}/.ebms-stub/keystore.jks
	signature.keystore.path=${user.home}/.ebms-stub/keystore.jks
	
	dir.base=H:/tmp/ebms-stub/overheid
	
	jmx.port=1099

========================
Start digipoort EbMS stub =
========================
> mule -config nl/clockwork/mule/ebms/stub/ebf/main.digipoort.xml


=======================
Start overheid EbMS stub =
=======================
> mule -config nl/clockwork/mule/ebms/stub/ebf/main.overheid.xml


===============
Testing Stubs =
===============
Load CPA in overheid EbMS stub:
	copy ebms-adapter-x.x.x.zip/resources/CPAs/cpaStubEBF.xml to <overheid EbMS stub dir.base>/cpa
		the file will be moved to <overheid EbMS stub dir.base>/cpa/processed when it's processed
		an import report will be written to <overheid EbMS stub dir.base>/cpa/reports
Now the CPA is loaded the overheid EbMS stub is ready to send and receive messages.

Load CPA in digipoort EbMS stub:
	copy ebms-adapter-x.x.x.zip/resources/CPAs/cpaStubEBF.xml to <digipoort EbMS stub dir.base>/cpa
		the file will be moved to <digipoort EbMS stub dir.base>/cpa/processed when it's processed
		an import report will be written to <digipoort EbMS stub dir.base>/cpa/reports
Now the CPA is loaded the digipoort EbMS stub is ready to send and receive messages.

Send afleverbericht message from digipoort EbMS stub to overheid EbMS stub:
	copy ebms-adapter-x.x.x.zip/resources/data/afleveren/Afleverbericht_Afleveren_ebMS_2.0_v1.1.xml to <digipoort EbMS stub dir.base>/afleveren/request
		check <digipoort EbMS stub dir.base>/afleveren/response for the response message

Send aanleverbericht message from overheid EbMS stub to digipoort EbMS stub:
	copy ebms-adapter-x.x.x.zip/resources/data/aanleveren/Aanleverbericht_Aanleveren_ebMS_2.0_v1.1.xml to <overheid EbMS stub dir.base>/aanleveren/request
		check <overheid EbMS stub dir.base>/aanleveren/request for the response message

=======
Usage =
=======
If you want to use the ebms-adapter in your own application you can include this project in your project and configure the adapter into your application.
Use nl/clockwork/mule/ebms/stub/ebf/main.overheid.xml as a starting point.
You will have to generate your own CPAs and application flow.

At the defined endpoint the application will receive an object of type EbMSMessageContent that contains:
- EbMSMessageContext (needed to reply on this message)
- properties (contain the properties from the EbMS Header defined in application property ebms.message.header.properties
- attachments (the actual EbMS Message content)

The application can instantiate a new message or reply to a received message by calling the vm endpoint ebms.message.send.in.
The application should wrap the content of the message in an object of type EbMSMessageContent as attachments.
If the message is a response to a previous received message, then include the EbMSMessageContext of the previous message.
The EbMS adapter will then correlate these two messages.
If the message is a new message, then leave the EbMSMessageContext empty.

You can use nl/clockwork/mule/ebms/stub/ebf/main.digipoort.xml as a Stub to test your own application.


=====================================
EbMS Adapter Communication Protocol =
=====================================
The EbMS adapter supports to different protocols:
- HTTP
- HTTPS

You can configure them by including the right xml in your project:
- nl/clockwork/mule/ebms/components/connector.http.xml
- nl/clockwork/mule/ebms/components/connector.https.xml

And you have to configure the right server protocol:
- service.protocol=http
- service.protocol=https

=======================
EbMS Adapter Database =
=======================
The EbMS adapter supports different databases:
- HSQLDB
- MSSQL

You can configure them by including the right xml in your project:
- nl/clockwork/mule/ebms/components/dao.hsqldb.xml
- nl/clockwork/mule/ebms/components/dao.mssql.xml

And you have to configure the right driver and connection string:
- ebms.jdbc.driverClassName=org.hsqldb.jdbcDriver
	ebms.jdbc.url=jdbc:hsqldb:mem:<dbname>
	or
	ebms.jdbc.url=jdbc:hsqldb:file:<path>
- ezpoort.jdbc.driverClassName=com.microsoft.sqlserver.jdbc.SQLServerDriver
	or
	ezpoort.jdbc.driverClassName=net.sourceforge.jtds.jdbc.Driver
	ezpoort.jdbc.url=jdbc:sqlserver://<host>:<port>;databaseName=<dbname>;

If you want to let the adapter use the application datasource exclude the following file:
- nl/clockwork/mule/ebms/components/datasource.xml
and add the name ebMSDataSource to the application datasource 

===========
Resources =
===========
the reources directory resides in ebms-adapter-x.x.x.zip/resources and contains the following data:
	CPAs - contains test CPAs
	data/aanleveren - contains aanleverbericht test messages
	data/afleveren - contains afleverbericht test messages
	scripts/database/hsqldb - contains hsqldb scripts
	scripts/database/mssql - contains mssql scripts

==============
Known Issues =
==============

During startup the following errors/warnings may occur and can be ignored:

WARN  nl.clockwork.mule.ebms.HSQLDatabaseProvider - java.sql.SQLException: object name already exists: CPA in statement [CREATE TABLE cpa
ERROR org.mule.module.management.mbean.ServiceService - Error post-registering the MBean javax.management.MalformedObjectNameException: Invalid character ':' in value part of property

=================================
Important configuration changes =
=================================
ebms.message.send.in changed to ebms.content.receive.in


==============
Restrictions =
==============

- Only HTTP(S) protocol is supported
- Only 1 Channel per Action is allowed
- Actor NextMSH is not supported. Only actor ToPartyMSH is supported
- Message Acknowledgments as part of another message are not supported. Only Aknowledgment messages with a MessageHeader containing service 'urn:oasis:names:tc:ebxml-msg:service' and action 'Acknowledgment' are supported
- Only supports one Transport at the moment
