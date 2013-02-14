Tomcat SSL Connector Configuration:

    <Connector port="8443" protocol="HTTP/1.1" SSLEnabled="true"
               maxThreads="150" scheme="https" secure="true"
               keystoreFile="f:/keystore.jks" keystorePass="password"
               truststoreFile="f:/keystore.jks" truststorePass="password"
               clientAuth="true" sslProtocol="TLS" 
               ciphers="TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_128_CBC_SHA"/>