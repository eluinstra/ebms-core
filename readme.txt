==============
Introduction =
==============

This version will support the full Digikoppeling EbMS Deployment Profile (see Koppelvlakstandaard ebMS Voor Digikoppeling 2.0 Versie 2.2) which is a subset of the ebXML Message Service 2.0 specification by OASIS.

This ebms-adapter runs on mule-standalone-2.2.1. You can use the ebms-adapter in two ways:
- include it in your application/project (only possible if your project also runs on mule-standalone-2.2.1)
- run it standalone and use the tcp bridge to connect to your application

This project includes 2 stubs (local and remote) that implement the AfleverService and the AanleverService. These stubs can communicate with each other.
The project also includes a standalone ebms-adapter configuration that has a tcp bridge to communicate with an application.
The project includes a standalone version of the local stub that uses the standalone ebms-adapter (through tcp), that can also communicate with the remote stub.
You can use the local (standalone) stub as a starting point for your project.

===============
Prerequisites =
===============
install mule-standalone-2.2.1
build or download ebms-adapter-1.0.0.jar
move the ebms-adapter-1.0.0.jar to <mule-standalone-2.2.1>/lib/user

download and move c3p0-0.9.1.2.jar and hsqldb-2.1.0.jar to <mule-standalone-2.2.1>/lib/opt


=================
Configure Stubs =
=================
create directory ${user.home}/.ebms-stub

copy ebms-adapter-1.0.0.jar/keystore.jks to ${user.home}/.ebms-stub

	============================
	Configure remote EbMS stub =
	============================
	create file ${user.home}/.ebms-stub/ebf.remote.properties (this overrides properties from nl/clockwork/mule/ebms/default.properties and nl/clockwork/mule/ebms/stub/ebf/default.remote.properties)
	
	edit and add the following lines to ${user.home}/.ebms-stub/ebf.remote.properties:
	
	service.port=4443
	service.remote.port=443
	
	server.keystore.path=${user.home}/.ebms-stub/keystore.jks
	client.keystore.path=${user.home}/.ebms-stub/keystore.jks
	truststore.path=${user.home}/.ebms-stub/keystore.jks
	signature.keystore.path=${user.home}/.ebms-stub/keystore.jks
	
	dir.base=H:/tmp/ebms-stub/remote
	
	jmx.port=1099
	
	===========================
	Configure local EbMS stub =
	===========================
	create file ${user.home}/.ebms-stub/ebf.local.properties (this overrides properties from nl/clockwork/mule/ebms/default.properties and nl/clockwork/mule/ebms/stub/ebf/default.local.properties)
	
	edit and add the following lines to ${user.home}/.ebms-stub/ebf.local.properties:
	
	service.port=443
	service.remote.port=4443
	
	server.keystore.path=${user.home}/.ebms-stub/keystore.jks
	client.keystore.path=${user.home}/.ebms-stub/keystore.jks
	truststore.path=${user.home}/.ebms-stub/keystore.jks
	signature.keystore.path=${user.home}/.ebms-stub/keystore.jks
	
	dir.base=H:/tmp/ebms-stub/local
	
	jmx.port=1099

========================
Start remote EbMS stub =
========================
> mule -config nl/clockwork/mule/ebms/stub/ebf/main.remote.xml


=======================
Start local EbMS stub =
=======================
> mule -config nl/clockwork/mule/ebms/stub/ebf/main.local.xml


===============
Testing Stubs =
===============
Load CPA in local EbMS stub:
	copy ebms-adapter-x.x.x.zip/resources/CPAs/cpaStubEBF.xml to <local EbMS stub dir.base>/cpa
		the file will be moved to <local EbMS stub dir.base>/cpa/processed when it's processed
		an import report will be written to <local EbMS stub dir.base>/cpa/reports
Load Routing Rules in local EbMS stub:
	copy ebms-adapter-x.x.x.zip/resources/data/routing.cpaStubEBF.local.sql to <local EbMS stub dir.base>/sql
		the file will be moved to <local EbMS stub dir.base>/sql/processed when it's processed
		an import report will be written to <local EbMS stub dir.base>/sql/reports
Now the CPA and Routing Rules are loaded the local EbMS stub is ready to send and receive messages.

Load CPA in remote EbMS stub:
	copy ebms-adapter-x.x.x.zip/resources/CPAs/cpaStubEBF.xml to <remote EbMS stub dir.base>/cpa
		the file will be moved to <remote EbMS stub dir.base>/cpa/processed when it's processed
		an import report will be written to <remote EbMS stub dir.base>/cpa/reports
Load Routing Rules in remote EbMS stub:
	copy ebms-adapter-x.x.x.zip/resources/data/routing.cpaStubEBF.remote.sql to <remote EbMS stub dir.base>/sql
		the file will be moved to <remote EbMS stub dir.base>/sql/processed when it's processed
		an import report will be written to <remote EbMS stub dir.base>/sql/reports
Now the CPA and Routing Rules are loaded the remote EbMS stub is ready to send and receive messages.

Send afleverbericht message from remote EbMS stub to local EbMS stub:
	copy ebms-adapter-x.x.x.zip/resources/data/afleveren/Afleverbericht_Afleveren_ebMS_2.0_v1.1.xml to <remote EbMS stub dir.base>/afleveren/request
		check <remote EbMS stub dir.base>/afleveren/response for the response message

Send aanleverbericht message from local EbMS stub to remote EbMS stub:
	copy ebms-adapter-x.x.x.zip/resources/data/aanleveren/Aanleverbericht_Aanleveren_ebMS_2.0_v1.1.xml to <local EbMS stub dir.base>/aanleveren/request
		check <local EbMS stub dir.base>/aanleveren/request for the response message

=======
Usage =
=======
If you want to use the ebms-adapter in your own application you can include this project in your project and configure the adapter into your application.
Use nl/clockwork/mule/ebms/stub/ebf/main.local.xml as a starting point.
You will have to generate your own CPAs and application flow, so you have to generate your own Routing Rules and load them into table ebms_channel:
	id								- primary key
	channel_id				- channel name (application uses as a reference)
	cpa_id						- CPA_ID of the CPA this channel/rule applies to
	action_id					- ActionBindingId from CPA this channel/rule applies to
	endpoint					- Application endpoint this channel should route to  

At the defined endpoint the application will receive an object of type EbMSMessageContent that contains:
- EbMSMessageContext (needed to reply on this message)
- properties (contain the properties from the EbMS Header defined in application property ebms.message.header.properties
- attachments (the actual EbMS Message content)

The application can instantiate a new message or reply to a received message by calling the vm endpoint ebms.message.send.in.
The message property EBMS.EBMS_CHANNEL_ID has to be set to the predefined channel_id the message has to be sent to.
The application should wrap the content of the message in an object of type EbMSMessageContent as attachments.
If the message is a response to a previous received message, then include the EbMSMessageContext of the previous message.
The EbMS adapter will then correlate these two messages.
If the message is a new message, then leave the EbMSMessageContext empty.

You can use nl/clockwork/mule/ebms/stub/ebf/main.remote.xml as a Stub to test your own application.


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
	data - contains routing data scripts
	data/aanleveren - contains aanleverbericht test messages
	data/afleveren - contains afleverbericht test messages
	scripts/database/hsqldb - contains hsqldb scripts
	scripts/database/mssql - contains mssql scripts
