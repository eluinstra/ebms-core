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
package nl.clockwork.ebms.api.cpa.soap;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.List;
import nl.clockwork.ebms.api.cpa.exception.CPAControllerException;

@WebService(name = "CPAService", targetNamespace = "http://www.ordina.nl/cpa/2.18", serviceName = "CPAService", portName = "CPAPort")
public interface CPAController
{
	/**
	 * Validates CPA cpa
	 * 
	 * @param cpa
	 * @throws CPAControllerException
	 */
	@WebMethod(operationName = "validateCPA")
	void validateCPA(@WebParam(name = "cpa") @XmlElement(required = true) /* CollaborationProtocolAgreement */String cpa) throws CPAControllerException;

	/**
	 * Stores CPA cpa. If overwrite is true and the CPA exists, the CPA will be overwritten
	 * 
	 * @param cpa
	 * @param overwrite
	 * @return The cpaId of the CPA
	 * @throws CPAControllerException
	 */
	@WebResult(name = "cpaId")
	@WebMethod(operationName = "insertCPA")
	String insertCPA(
			@WebParam(name = "cpa") @XmlElement(required = true) /* CollaborationProtocolAgreement */String cpa,
			@WebParam(name = "overwrite") Boolean overwrite) throws CPAControllerException;

	/**
	 * Removes CPA identified by cpaId
	 * 
	 * @param cpaId
	 * @throws CPAControllerException
	 */
	@WebMethod(operationName = "deleteCPA")
	void deleteCPA(@WebParam(name = "cpaId") @XmlElement(required = true) String cpaId) throws CPAControllerException;

	/**
	 * Returns a list of all cpaIds
	 * 
	 * @return The list of cpaIds
	 * @throws CPAControllerException
	 */
	@WebResult(name = "cpaId")
	@WebMethod(operationName = "getCPAIds")
	List<String> getCPAIds() throws CPAControllerException;

	/**
	 * Returns the CPA identified by cpaId
	 * 
	 * @param cpaId
	 * @return The CPA
	 * @throws CPAControllerException
	 */
	@WebResult(name = "cpa")
	@WebMethod(operationName = "getCPA")
	/* CollaborationProtocolAgreement */String getCPA(@WebParam(name = "cpaId") @XmlElement(required = true) String cpaId) throws CPAControllerException;

	/**
	 * Deletes the CPA cache
	 * 
	 * @throws CPAControllerException
	 */
	@WebMethod(operationName = "deleteCache")
	void deleteCache() throws CPAControllerException;
}
