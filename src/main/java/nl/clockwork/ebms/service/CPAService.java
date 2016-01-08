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
package nl.clockwork.ebms.service;

import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlElement;

@WebService(targetNamespace="http://www.ordina.nl/cpa/2.3")
public interface CPAService
{
	@WebMethod(operationName="ValidateCPA")
	void validateCPA(@WebParam(name="CPA") @XmlElement(required=true) /*CollaborationProtocolAgreement*/String cpa) throws CPAServiceException;

	@WebResult(name="CPAId")
	@WebMethod(operationName="InsertCPA")
	String insertCPA(@WebParam(name="CPA") @XmlElement(required=true) /*CollaborationProtocolAgreement*/String cpa, @WebParam(name="URL") String url, @WebParam(name="Overwrite") Boolean overwrite) throws CPAServiceException;

	@WebMethod(operationName="DeleteCPA")
	void deleteCPA(@WebParam(name="CPAId") @XmlElement(required=true) String cpaId) throws CPAServiceException;

	@WebResult(name="CPAIds")
	@WebMethod(operationName="GetCPAIds")
	//@ResponseWrapper(className="nl.clockwork.ebms.service.GetCPAIdsResponse")
	List<String> getCPAIds() throws CPAServiceException;

	@WebResult(name="CPA")
	@WebMethod(operationName="GetCPA")
	/*CollaborationProtocolAgreement*/String getCPA(@WebParam(name="CPAId") @XmlElement(required=true) String cpaId) throws CPAServiceException;

	@WebResult(name="URL")
	@WebMethod(operationName="GetURL")
	String getURL(@WebParam(name="CPAId") @XmlElement(required=true) String cpaId) throws CPAServiceException;

	@WebMethod(operationName="SetURL")
	void setURL(@WebParam(name="CPAId") @XmlElement(required=true) String cpaId, @WebParam(name="URL") @XmlElement(required=true) String url) throws CPAServiceException;

}
