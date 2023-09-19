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
package nl.clockwork.ebms.cpa;

import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CPAEndpointConfig
{
	public static final QName SERVICE_NAME = new QName("http://www.ordina.nl/cpa/2.18", "CPAService");
	public static final QName PORT_NAME = new QName("http://www.ordina.nl/cpa/2.18", "CPAPort");
	public static final String SERVICE_ENDPOINT = "http://localhost:8080/service/cpa";

	@Bean(name = "cpaEndpoint")
	Endpoint publishEndpoint(CPAService cpaService)
	{
		return Endpoint.publish(SERVICE_ENDPOINT, cpaService);
	}
}
