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
package nl.clockwork.ebms;


import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.soap.SOAPBinding;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.cpa.CPAService;
import nl.clockwork.ebms.cpa.certificate.CertificateMappingService;
import nl.clockwork.ebms.cpa.url.URLMappingService;
import nl.clockwork.ebms.event.MessageEventListenerConfig.EventListenerType;
import nl.clockwork.ebms.service.EbMSMessageService;
import nl.clockwork.ebms.service.EbMSMessageServiceMTOM;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmbeddedWebConfig
{
	@Value("${eventListener.type}")
	EventListenerType eventListenerType;
	@Value("#{'${ebms.cors.allowOrigins}'.split(',')}")
	List<String> allowOrigins;
	@Autowired
	CPAService cpaService;
	@Autowired
	URLMappingService urlMappingService;
	@Autowired
	CertificateMappingService certificateMappingService;
	@Autowired
	EbMSMessageService ebMSMessageService;
	@Autowired
	EbMSMessageServiceMTOM ebMSMessageServiceMTOM;

	@Bean
	public Endpoint cpaServiceEndpoint()
	{
		return publishEndpoint(cpaService, "/cpa", "http://www.ordina.nl/cpa/2.18", "CPAService", "CPAPort");
	}

	@Bean
	public Endpoint urlMappingServiceEndpoint()
	{
		return publishEndpoint(urlMappingService, "/urlMapping", "http://www.ordina.nl/cpa/urlMapping/2.18", "URLMappingService", "URLMappingPort");
	}

	@Bean
	public Endpoint certificateMappingServiceEndpoint()
	{
		return publishEndpoint(
				certificateMappingService,
				"/certificateMapping",
				"http://www.ordina.nl/cpa/certificateMapping/2.18",
				"CertificateMappingService",
				"CertificateMappingPort");
	}

	@Bean
	public Endpoint ebMSMessageServiceEndpoint()
	{
		return publishEndpoint(ebMSMessageService, "/ebms", "http://www.ordina.nl/ebms/2.18", "EbMSMessageService", "EbMSMessagePort");
	}

	@Bean
	public Endpoint ebMSMessageServiceMTOMEndpoint()
	{
		val result = new EndpointImpl(cxf(), ebMSMessageServiceMTOM);
		result.setAddress("/ebmsMTOM");
		result.setServiceName(new QName("http://www.ordina.nl/ebms/2.18", "EbMSMessageService"));
		result.setEndpointName(new QName("http://www.ordina.nl/ebms/2.18", "EbMSMessagePort"));
		result.publish();
		enableMTOM(result);
		return result;
	}

	private void enableMTOM(final org.apache.cxf.jaxws.EndpointImpl result)
	{
		val binding = (SOAPBinding)result.getBinding();
		binding.setMTOMEnabled(true);
	}

	@Bean
	public SpringBus cxf()
	{
		val result = new SpringBus();
		result.setFeatures(Collections.singletonList(createLoggingFeature()));
		return result;
	}

	private LoggingFeature createLoggingFeature()
	{
		val result = new LoggingFeature();
		result.setPrettyLogging(true);
		return result;
	}

	private Endpoint publishEndpoint(Object service, String address, String namespaceUri, String serviceName, String endpointName)
	{
		val result = new EndpointImpl(cxf(), service);
		result.setAddress(address);
		result.setServiceName(new QName(namespaceUri, serviceName));
		result.setEndpointName(new QName(namespaceUri, endpointName));
		result.publish();
		return result;
	}
}
