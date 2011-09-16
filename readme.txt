==============
Introduction =
==============

This version will support the full Digikoppeling EbMS Deployment Profile (see Koppelvlakstandaard ebMS Voor Digikoppeling 2.0 Versie 2.2) which is a subset of the ebXML Message Service 2.0 specification by OASIS.


===============
Prerequisites =
===============
install mule-standalone-2.2.1
build or download ebms-adapter-1.0.0.jar
move the ebms-adapter-1.0.0.jar to <mule-standalone-2.2.1>/lib/user

download and move c3p0-0.9.1.2.jar and hsqldb-2.1.0.jar to <mule-standalone-2.2.1>/lib/opt


===============
Configuration =
===============
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
> mule -config main.stub.ebf.remote.xml


=======================
Start local EbMS stub =
=======================
> mule -config main.stub.ebf.local.xml


=======
Usage =
=======
Load CPA in remote EbMS stub:
	copy ebms-adapter-x.x.x.zip/resources/CPAs/cpaStubEBF.xml to <remote EbMS stub dir.base>/cpa
		the file will be moved to <remote EbMS stub dir.base>/cpa/processed when it's processed
		an import report will be written to <remote EbMS stub dir.base>/cpa/reports
Now the CPA is loaded and the remote EbMS stub is ready to send and receive messages.

Load CPA in local EbMS stub:
	copy ebms-adapter-x.x.x.zip/resources/CPAs/cpaStubEBF.xml to <local EbMS stub dir.base>/cpa
		the file will be moved to <local EbMS stub dir.base>/cpa/processed when it's processed
		an import report will be written to <local EbMS stub dir.base>/cpa/reports
Now the CPA is loaded and the local EbMS stub is ready to send and receive messages.

Send afleverbericht message from remote EbMS stub to local EbMS stub:
	copy ebms-adapter-x.x.x.zip/resources/data/afleveren/Afleverbericht_Afleveren_ebMS_2.0_v1.1.xml to <remote EbMS stub dir.base>/afleveren/request
		check <remote EbMS stub dir.base>/afleveren/response for the response message

Send aanleverbericht message from local EbMS stub to remote EbMS stub:
	copy ebms-adapter-x.x.x.zip/resources/data/aanleveren/Aanleverbericht_Aanleveren_ebMS_2.0_v1.1.xml to <local EbMS stub dir.base>/aanleveren/request
		check <local EbMS stub dir.base>/aanleveren/request for the response message

===========
Resources =
===========
the reources directory resides in ebms-adapter-x.x.x.zip/resources and contains the following data:
	CPAs - contains test CPAs
	data/aanleveren - contains aanleverbericht test messages
	data/afleveren - contains afleverbericht test messages
	scripts/database/hsqldb - contains hsqldb scripts
	scripts/database/mssql - contains mssql scripts
