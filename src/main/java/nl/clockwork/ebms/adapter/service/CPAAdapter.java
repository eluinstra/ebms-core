/*******************************************************************************
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
 ******************************************************************************/
package nl.clockwork.ebms.adapter.service;

import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

@WebService(targetNamespace="http://www.clockwork.nl/cpa/adapter/1.0")
public interface CPAAdapter
{
	@WebResult(name="Result")
	@WebMethod(operationName="InsertCPA")
	boolean insertCPA(@WebParam(name="CPA") /*CollaborationProtocolAgreement*/String cpa, @WebParam(name="Overwrite") Boolean overwrite);

	@WebResult(name="Result")
	@WebMethod(operationName="DeleteCPA")
	boolean deleteCPA(@WebParam(name="CPAId") String cpaId);

	@WebResult(name="MessageIds")
	@WebMethod(operationName="GetCPAIds")
	List<String> getCPAIds();

	@WebResult(name="CPA")
	@WebMethod(operationName="GetCPA")
	/*CollaborationProtocolAgreement*/String getCPA(@WebParam(name="CPAId") String cpaId);

}
