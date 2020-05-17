/**
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
package nl.clockwork.ebms.service.cpa.url;

import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlElement;

@WebService(targetNamespace="http://www.ordina.nl/cpa/urlMapping/2.17")
public interface URLMappingService
{
	/**
	 * Stores URL mapping urlMapping in the database
	 * 
	 * @param urlMapping - Maps the source URL to the destination URL
	 * @throws URLMappingServiceException
	 */
	@WebMethod(operationName="SetURLMapping")
	void setURLMapping(@WebParam(name="URLMapping") @XmlElement(required=true) URLMapping urlMapping) throws URLMappingServiceException;

	/**
	 * Removes URL mapping identified by source URL source from the database
	 * 
	 * @param source
	 * @throws URLMappingServiceException
	 */
	@WebMethod(operationName="DeleteURLMapping")
	void deleteURLMapping(@WebParam(name="SourceURL") @XmlElement(required=true) String source) throws URLMappingServiceException;

	/**
	 * Gets all URL mappings that are stored in the database
	 * 
	 * @return The list of URL mappings
	 * @throws URLMappingServiceException
	 */
	@WebResult(name="URLs")
	@WebMethod(operationName="GetURLMappings")
	List<URLMapping> getURLMappings() throws URLMappingServiceException;
}
