/*
 * Copyright 2011 Clockwork
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.clockwork.ebms.cpa.url;


import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class URLMappingEndpointConfig
{
	static final QName SERVICE_NAME = new QName("http://www.ordina.nl/cpa/urlMapping/2.18", "UrlMappingService");
	static final QName PORT_NAME = new QName("http://www.ordina.nl/cpa/urlMapping/2.18", "UrlMappingPort");
	static final String SERVICE_ENDPOINT = "http://localhost:8080/service/urlMapping";

	@Bean(name = "urlMappingEndpoint")
	Endpoint publishEndpoint(URLMappingService mappingService)
	{
		return Endpoint.publish(SERVICE_ENDPOINT, mappingService);
	}
}
