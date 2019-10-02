EbMS Service URL: http://localhost:8080/ebms-adapter-web/service
EbMS URL: https://localhost:8443/ebms-adapter-web/ebms

Tomcat SSL Connector Configuration:
- place the keystore and truststore in $CATALINA_HOME/conf
- configure the following connector in $CATALINA_HOME/conf/server.xml:
<Connector port="8443" protocol="HTTP/1.1" SSLEnabled="true"
           maxThreads="150" scheme="https" secure="true"
           keystoreFile="conf/keystore.jks" keystorePass="password"
           truststoreFile="conf/truststore.jks" truststorePass="password"
           clientAuth="true" sslProtocol="TLSv1.2" 
           ciphers="TLS_RSA_WITH_AES_256_CBC_SHA256,TLS_RSA_WITH_AES_128_CBC_SHA256,TLS_RSA_WITH_AES_256_CBC_SHA,TLS_RSA_WITH_AES_128_CBC_SHA"/>