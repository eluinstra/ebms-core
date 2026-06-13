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
package nl.clockwork.ebms.server.embedded.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.soap.SOAPBinding;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.api.certificate.rest.CertificateMappingRestController;
import nl.clockwork.ebms.api.certificate.soap.CertificateMappingController;
import nl.clockwork.ebms.api.cpa.rest.CPARestController;
import nl.clockwork.ebms.api.cpa.soap.CPAController;
import nl.clockwork.ebms.api.ebms.rest.EbMSRestController;
import nl.clockwork.ebms.api.ebms.soap.EbMSController;
import nl.clockwork.ebms.api.ebms.soap.EbMSControllerMTOM;
import nl.clockwork.ebms.api.url.rest.URLMappingRestController;
import nl.clockwork.ebms.api.url.soap.URLMappingController;
import nl.clockwork.ebms.server.embedded.dao.EbMSDAO;
import nl.clockwork.ebms.server.embedded.web.admin.AdminRestController;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxrs.JAXRSBindingFactory;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.rs.security.cors.CrossOriginResourceSharingFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmbeddedWebConfig
{
	@Value("#{'${ebms.cors.allowOrigins}'.split(',')}")
	List<String> allowOrigins;

	@Bean
	public Endpoint cpaServiceEndpoint(CPAController cpaController)
	{
		return publishEndpoint(cpaController, "/cpa", "http://www.ordina.nl/cpa/2.18", "CPAService", "CPAPort");
	}

	@Bean
	public Endpoint urlMappingServiceEndpoint(URLMappingController urlMappingService)
	{
		return publishEndpoint(urlMappingService, "/urlMapping", "http://www.ordina.nl/cpa/urlMapping/2.18", "URLMappingService", "URLMappingPort");
	}

	@Bean
	public Endpoint certificateMappingServiceEndpoint(CertificateMappingController certificateMappingService)
	{
		return publishEndpoint(
				certificateMappingService,
				"/certificateMapping",
				"http://www.ordina.nl/cpa/certificateMapping/2.18",
				"CertificateMappingService",
				"CertificateMappingPort");
	}

	@Bean
	public Endpoint ebMSMessageServiceEndpoint(EbMSController ebMSController)
	{
		return publishEndpoint(ebMSController, "/ebms", "http://www.ordina.nl/ebms/2.18", "EbMSMessageService", "EbMSMessagePort");
	}

	@Bean
	public Endpoint ebMSMessageServiceMTOMEndpoint(EbMSControllerMTOM ebMSMessageServiceMTOM)
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

	private org.apache.cxf.ext.logging.LoggingFeature createLoggingFeature()
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

	@Bean
	public Server createCPARestServer(CPARestController cpaRestService)
	{
		return createRestServer(CPARestController.class, cpaRestService, "/cpas");
	}

	@Bean
	public Server createURLMappingCPARestServer(URLMappingRestController urlMappingRestService)
	{
		return createRestServer(URLMappingRestController.class, urlMappingRestService, "/urlMappings");
	}

	@Bean
	public Server createCertificateMappingRestServer(CertificateMappingRestController certificateMappingRestService)
	{
		return createRestServer(CertificateMappingRestController.class, certificateMappingRestService, "/certificateMappings");
	}

	@Bean
	public Server createEbMSRestServer(EbMSRestController ebMSMessageRestService)
	{
		return createRestServer(EbMSRestController.class, ebMSMessageRestService, "/ebms");
	}

	@Bean
	public AdminRestController adminRestController(@Qualifier("ebMSAdminDAO") EbMSDAO ebMSDAO)
	{
		return new AdminRestController(ebMSDAO);
	}

	@Bean
	public Server createAdminRestServer(AdminRestController adminRestService)
	{
		return createRestServer(AdminRestController.class, adminRestService, "/admin");
	}

	public Server createRestServer(Class<?> resourceClass, Object resourceObject, String path)
	{
		val sf = new JAXRSServerFactoryBean();
		sf.setBus(cxf());
		sf.setAddress("/rest/v19" + path);
		sf.setProviders(Arrays.asList(createCrossOriginResourceSharingFilter(), createJacksonJsonProvider()));
		sf.setFeatures(Arrays.asList(createOpenApiFeature()));
		sf.setResourceClasses(resourceClass);
		sf.setResourceProvider(resourceClass, new SingletonResourceProvider(resourceObject));
		registerBindingFactory(sf.getBus());
		return sf.create();
	}

	private CrossOriginResourceSharingFilter createCrossOriginResourceSharingFilter()
	{
		val result = new CrossOriginResourceSharingFilter();
		result.setAllowOrigins(allowOrigins.stream().filter(StringUtils::isNotEmpty).toList());
		return result;
	}

	private JacksonJsonProvider createJacksonJsonProvider()
	{
		val result = new JacksonJsonProvider();
		result.setMapper(createObjectMapper());
		return result;
	}

	private ObjectMapper createObjectMapper()
	{
		val result = new ObjectMapper();
		result.registerModule(new Jdk8Module());
		result.registerModule(new JavaTimeModule());
		result.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		result.setDefaultPropertyInclusion(JsonInclude.Value.construct(Include.NON_NULL, Include.NON_NULL));
		return result;
	}

	private OpenApiFeature createOpenApiFeature()
	{
		val result = new OpenApiFeature();
		result.setUseContextBasedConfig(true);
		result.setScan(false);
		result.setSupportSwaggerUi(false);
		return result;
	}

	private void registerBindingFactory(final Bus bus)
	{
		val manager = bus.getExtension(BindingFactoryManager.class);
		manager.registerBindingFactory(JAXRSBindingFactory.JAXRS_BINDING_ID, createBindingFactory(bus));
	}

	private JAXRSBindingFactory createBindingFactory(final Bus bus)
	{
		val result = new JAXRSBindingFactory();
		result.setBus(bus);
		return result;
	}
}
