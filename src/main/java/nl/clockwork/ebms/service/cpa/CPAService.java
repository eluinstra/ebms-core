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
package nl.clockwork.ebms.service.cpa;

import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlElement;

@WebService(targetNamespace="http://www.ordina.nl/cpa/2.17")
public interface CPAService
{
	/**
	 * Validates CPA cpa
	 * 
	 * @param cpa
	 * @throws CPAServiceException
	 */
	@WebMethod(operationName="ValidateCPA")
	void validateCPA(@WebParam(name="CPA") @XmlElement(required=true) /*CollaborationProtocolAgreement*/String cpa) throws CPAServiceException;

	/**
	 * Stores CPA cpa in the database. If overwrite is true and the CPA exists, the CPA will be overwritten
	 * 
	 * @param cpa
	 * @param overwrite
	 * @return The cpaId of the CPA
	 * @throws CPAServiceException
	 */
	@WebResult(name="CPAId")
	@WebMethod(operationName="InsertCPA")
	String insertCPA(@WebParam(name="CPA") @XmlElement(required=true) /*CollaborationProtocolAgreement*/String cpa, @WebParam(name="Overwrite") Boolean overwrite) throws CPAServiceException;

	/**
	 * Removes CPA identified by cpaId from the database
	 * 
	 * @param cpaId
	 * @throws CPAServiceException
	 */
	@WebMethod(operationName="DeleteCPA")
	void deleteCPA(@WebParam(name="CPAId") @XmlElement(required=true) String cpaId) throws CPAServiceException;

	/**
	 * Returns all cpaIds that are stored in the database
	 * 
	 * @return The list of cpaIds
	 * @throws CPAServiceException
	 */
	@WebResult(name="CPAIds")
	@WebMethod(operationName="GetCPAIds")
	List<String> getCPAIds() throws CPAServiceException;

	/**
	 * Gets the CPA identified by cpaId from the database
	 * 
	 * @param cpaId
	 * @return The CPA
	 * @throws CPAServiceException
	 */
	@WebResult(name="CPA")
	@WebMethod(operationName="GetCPA")
	/*CollaborationProtocolAgreement*/String getCPA(@WebParam(name="CPAId") @XmlElement(required=true) String cpaId) throws CPAServiceException;
}
