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


import java.util.List;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlElement;

@WebService(name = "UrlMappingService", targetNamespace = "http://www.ordina.nl/cpa/urlMapping/2.18", serviceName = "UrlMappingService", portName = "UrlMappingPort")
public interface URLMappingService
{
	/**
	 * Stores URL mapping urlMapping
	 * 
	 * @param urlMapping - Maps the source URL to the destination URL
	 * @throws URLMappingServiceException
	 */
	@WebMethod(operationName = "setURLMapping")
	void setURLMapping(@WebParam(name = "urlMapping") @XmlElement(required = true) URLMapping urlMapping) throws URLMappingServiceException;

	/**
	 * Removes URL mapping identified by source URL source
	 * 
	 * @param source
	 * @throws URLMappingServiceException
	 */
	@WebMethod(operationName = "deleteURLMapping")
	void deleteURLMapping(@WebParam(name = "sourceURL") @XmlElement(required = true) String source) throws URLMappingServiceException;

	/**
	 * Returns a list of all URL mappings
	 * 
	 * @return The list of URL mappings
	 * @throws URLMappingServiceException
	 */
	@WebResult(name = "url")
	@WebMethod(operationName = "getURLMappings")
	List<URLMapping> getURLMappings() throws URLMappingServiceException;

	/**
	 * Deletes the URL mapping cache
	 * 
	 * @throws URLMappingServiceException
	 */
	@WebMethod(operationName = "deleteCache")
	void deleteCache() throws URLMappingServiceException;
}
