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


import java.util.List;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlElement;

@WebService(name = "CPAService", targetNamespace = "http://www.ordina.nl/cpa/2.18", serviceName = "CPAService", portName = "CPAPort")
public interface CPAService
{
	/**
	 * Validates CPA cpa
	 * 
	 * @param cpa
	 * @throws CPAServiceException
	 */
	@WebMethod(operationName = "validateCPA")
	void validateCPA(@WebParam(name = "cpa") @XmlElement(required = true) /* CollaborationProtocolAgreement */String cpa) throws CPAServiceException;

	/**
	 * Stores CPA cpa. If overwrite is true and the CPA exists, the CPA will be overwritten
	 * 
	 * @param cpa
	 * @param overwrite
	 * @return The cpaId of the CPA
	 * @throws CPAServiceException
	 */
	@WebResult(name = "cpaId")
	@WebMethod(operationName = "insertCPA")
	String insertCPA(
			@WebParam(name = "cpa") @XmlElement(required = true) /* CollaborationProtocolAgreement */String cpa,
			@WebParam(name = "overwrite") Boolean overwrite) throws CPAServiceException;

	/**
	 * Removes CPA identified by cpaId
	 * 
	 * @param cpaId
	 * @throws CPAServiceException
	 */
	@WebMethod(operationName = "deleteCPA")
	void deleteCPA(@WebParam(name = "cpaId") @XmlElement(required = true) String cpaId) throws CPAServiceException;

	/**
	 * Returns a list of all cpaIds
	 * 
	 * @return The list of cpaIds
	 * @throws CPAServiceException
	 */
	@WebResult(name = "cpaId")
	@WebMethod(operationName = "getCPAIds")
	List<String> getCPAIds() throws CPAServiceException;

	/**
	 * Returns the CPA identified by cpaId
	 * 
	 * @param cpaId
	 * @return The CPA
	 * @throws CPAServiceException
	 */
	@WebResult(name = "cpa")
	@WebMethod(operationName = "getCPA")
	/* CollaborationProtocolAgreement */String getCPA(@WebParam(name = "cpaId") @XmlElement(required = true) String cpaId) throws CPAServiceException;

	/**
	 * Deletes the CPA cache
	 * 
	 * @throws CPAServiceException
	 */
	@WebMethod(operationName = "deleteCache")
	void deleteCache() throws CPAServiceException;
}
