==============
Introduction =
==============

This version will support the full Digikoppeling EbMS Deployment Profile (see Koppelvlakstandaard ebMS Voor Digikoppeling 2.0 Versie 2.2) which is a subset of the ebXML Message Service 2.0 specification by OASIS.

This ebms-adapter runs on mule-standalone-3.x.x. You can use the ebms-adapter in two ways:
- include it in your application/project (only possible if your project also runs on mule-standalone-3.x.x)
- run it standalone and use the tcp bridge to connect to your application

This project includes 2 stubs (digipoort en overheid) that implement the AfleverService and the AanleverService. These stubs can communicate with each other.
The project also includes a standalone ebms-adapter configuration that has a tcp bridge to communicate with an application.
The project includes a standalone version of the overheid stub that uses the standalone ebms-adapter (through tcp), that can also communicate with the digipoort stub.
You can use the overheid (standalone) stub as a starting point for your project.

supported databases:
- hsqldb 2.1.0
- mysql (5.5) (innodb)
- postgresql (9)
- mssql 2008 R2
- oracle (11)(lob > 4k fix (needed for older oracle jdbc adapters) not implemented!)

===============
Prerequisites =
===============
install mule-standalone-3.x.x
build or download ebms-adapter-mule3-1.0.0.zip
move ebms-adapter-mule3-1.0.0.zip to <mule-standalone-3.x.x>/apps

download and copy one of the following libraries to <mule-standalone-3.x.x>/lib/opt depending on the database used:
- hsqldb-2.2.9.jar
- mysql-connector-java-5.1.18.jar
- postgresql-9.1-901.jdbc3.jar or postgresql-9.1-901.jdbc4.jar
- jtds-1.2.4.jar or sqljdbc4-201004.jar
- ojdbc6-11.2.0.1.0.jar (and orai18n-11.2.0.1.0.jar)

to generate cleaner/smaller messages/signatures download and copy xmlsec-1.5.3.jar to <mule-standalone-3.x.x>/lib/user (optional)

=================
Configure Stubs =
=================

	============================
	Configure digipoort EbMS stub =
	============================
	create file ${mule.home}/conf/ebf.digipoort.properties (this overrides properties from nl/clockwork/ebms/default.properties and nl/clockwork/mule/ebms/stub/ebf/default.digipoort.properties)
	
	edit and add the following lines to ${mule.home}/conf/ebf.digipoort.properties:
	
	service.port=4443
	
	dir.base=H:/tmp/ebms-stub/digipoort
	
	jmx.port=1099
	
	===========================
	Configure overheid EbMS stub =
	===========================
	create file ${mule.home}/conf/ebf.overheid.properties (this overrides properties from nl/clockwork/mule/ebms/default.properties and nl/clockwork/mule/ebms/stub/ebf/default.overheid.properties)
	
	edit and add the following lines to ${mule.home}/conf/ebf.overheid.properties:
	
	service.port=443
	
	dir.base=H:/tmp/ebms-stub/overheid
	
	jmx.port=1099

===================================
Configure EbMS Adapter Standalone =
===================================
create file ${mule.home}/conf/ebms.adapter.properties (this overrides properties from nl/clockwork/mule/ebms/default.properties)

	edit and add the following lines to ${mule.home}/conf/ebms.adapter.properties:
	
	service.port=80
	
	ebms.jdbc.driverClassName=org.hsqldb.jdbcDriver
	ebms.jdbc.url=jdbc:hsqldb:file:<path>

	dir.base=H:/tmp/ebms-adapter
	
	jmx.port=1099

===========================
Set Environment Vairables =
===========================
> set JAVA_HOME=<jdk6>
> set MULE_HOME=<mule-standalone-3.x.x>
> set PATH=%JAVA_HOME%\bin;%MULE_HOME%\bin;%PATH%

> cd %MULE_HOME%\bin

========================
Start digipoort EbMS stub =
========================
> mule -config nl/clockwork/mule/ebms/stub/ebf/main.digipoort.xml

=======================
Start overheid EbMS stub =
=======================
> mule -config nl/clockwork/mule/ebms/stub/ebf/main.overheid.xml

===============================
Start EbMS Adapter Standalone =
===============================
add the following lines to <mule-standalone-3.x.x>/conf/wrapper.conf
wrapper.java.additional.4=-Debms.protocol=http

> mule -config nl/clockwork/mule/ebms/main.xml

CPA Webservice:
http://localhost:8089/adapter/cpa

Messaging Webservice:
http://localhost:8089/adapter/ebms

===============
Testing Stubs =
===============
Load CPA in overheid EbMS stub:
	copy ebms-adapter-mule3-x.x.x.zip/resources/CPAs/cpaStubEBF.xml to <overheid EbMS stub dir.base>/cpa
		the file will be moved to <overheid EbMS stub dir.base>/cpa/processed when it's processed
		an import report will be written to <overheid EbMS stub dir.base>/cpa/reports
Now the CPA is loaded the overheid EbMS stub is ready to send and receive messages.

Load CPA in digipoort EbMS stub:
	copy ebms-adapter-mule3-x.x.x.zip/resources/CPAs/cpaStubEBF.xml to <digipoort EbMS stub dir.base>/cpa
		the file will be moved to <digipoort EbMS stub dir.base>/cpa/processed when it's processed
		an import report will be written to <digipoort EbMS stub dir.base>/cpa/reports
Now the CPA is loaded the digipoort EbMS stub is ready to send and receive messages.

Send afleverbericht message from digipoort EbMS stub to overheid EbMS stub:
	copy ebms-adapter-mule3-x.x.x.zip/resources/data/afleveren/Afleverbericht_Afleveren_ebMS_2.0_v1.1.xml to <digipoort EbMS stub dir.base>/afleveren/request
		check <digipoort EbMS stub dir.base>/afleveren/response for the response message

Send aanleverbericht message from overheid EbMS stub to digipoort EbMS stub:
	copy ebms-adapter-mule3-x.x.x.zip/resources/data/aanleveren/Aanleverbericht_Aanleveren_ebMS_2.0_v1.1.xml to <overheid EbMS stub dir.base>/aanleveren/request
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
- nl/clockwork/mule/ebms/flows/ebms.endpoint.http.xml
- nl/clockwork/mule/ebms/flows/ebms.endpoint.https.xml

=======================
EbMS Adapter Database =
=======================
The EbMS adapter supports different databases:
- HSQLDB
- MySQL
- PostgreSQL
- MSSQL
- Oracle

You can configure them by including the right xml from ebms-core-1.0.0.jar in your project:
- nl/clockwork/ebms/datasource.xml
- nl/clockwork/ebms/dao.spring.xml

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
- nl/clockwork/ebms/dao/spring/datasource.xml
and add the name ebMSDataSource to the application datasource 

===========
Resources =
===========
the reources directory resides in ebms-adapter-mule3-x.x.x-src.zip/resources and contains the following data:
	CPAs - contains test CPAs
	data/aanleveren - contains aanleverbericht test messages
	data/afleveren - contains afleverbericht test messages

the database resources directory resides in ebms-core-x.x.x-src.zip/resources and contains the following data:
	scripts/database/master/hsqldb - contains hsqldb scripts
	scripts/database/master/mssql - contains mssql scripts
	scripts/database/master/mysql - contains mysql scripts
	scripts/database/master/oracle - contains oracle scripts
	scripts/database/master/postgresql - contains postgresql scripts

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

==============
Known Issues =
==============

