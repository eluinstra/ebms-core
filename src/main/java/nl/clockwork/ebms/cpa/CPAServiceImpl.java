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


import java.io.IOException;
import java.util.List;
import javax.xml.bind.JAXBException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import nl.clockwork.ebms.jaxb.JAXBParser;
import nl.clockwork.ebms.validation.CPAValidator;
import nl.clockwork.ebms.validation.XSDValidator;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.xml.sax.SAXException;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class CPAServiceImpl implements CPAService
{
	@NonNull
	CPAManager cpaManager;
	@NonNull
	CPAValidator cpaValidator;
	XSDValidator xsdValidator = new XSDValidator("/nl/clockwork/ebms/xsd/cpp-cpa-2_0.xsd");

	@Override
	public void validateCPA(String cpa) throws CPAServiceException
	{
		try
		{
			validateCPAImpl(cpa);
		}
		catch (CPAServiceException e)
		{
			log.error("ValidateCPA\n" + cpa,e);
			throw e;
		}
		catch (Exception e)
		{
			log.error("ValidateCPA\n" + cpa,e);
			throw new CPAServiceException(e);
		}
	}

	protected void validateCPAImpl(String cpa) throws SAXException, IOException, JAXBException
	{
		log.debug("ValidateCPA");
		xsdValidator.validate(cpa);
		val parsedCpa = JAXBParser.getInstance(CollaborationProtocolAgreement.class).handleUnsafe(cpa);
		log.info("Validating CPA " + parsedCpa.getCpaid());
		cpaValidator.validate(parsedCpa);
	}

	@Override
	public String insertCPA(String cpa, Boolean overwrite) throws CPAServiceException
	{
		try
		{
			return insertCPAImpl(cpa,overwrite);
		}
		catch (CPAServiceException e)
		{
			log.error("InsertCPA\n" + cpa,e);
			throw e;
		}
		catch (Exception e)
		{
			log.error("InsertCPA\n" + cpa,e);
			throw new CPAServiceException(e);
		}
	}

	protected String insertCPAImpl(String cpa, Boolean overwrite) throws SAXException, IOException, JAXBException
	{
		log.debug("InsertCPA");
		xsdValidator.validate(cpa);
		val parsedCpa = JAXBParser.getInstance(CollaborationProtocolAgreement.class).handleUnsafe(cpa);
		new CPAValidator(cpaManager).validate(parsedCpa);
		cpaManager.setCPA(parsedCpa,overwrite);
		log.debug("InsertCPA done");
		return parsedCpa.getCpaid();
	}

	@Override
	public void deleteCPA(String cpaId) throws CPAServiceException
	{
		try
		{
			deleteCPAImpl(cpaId);
		}
		catch (CPAServiceException e)
		{
			log.error("DeleteCPA " + cpaId,e);
			throw e;
		}
		catch (Exception e)
		{
			log.error("DeleteCPA " + cpaId,e);
			throw new CPAServiceException(e);
		}
	}

	protected void deleteCPAImpl(String cpaId)
	{
		log.debug("DeleteCPA " + cpaId);
		if (cpaManager.deleteCPA(cpaId) == 0)
			throw new CPANotFoundException();
	}

	@Override
	public List<String> getCPAIds() throws CPAServiceException
	{
		try
		{
			return getCPAIdsImpl();
		}
		catch (CPAServiceException e)
		{
			log.error("GetCPAIds",e);
			throw e;
		}
		catch (Exception e)
		{
			log.error("GetCPAIds",e);
			throw new CPAServiceException(e);
		}
	}

	protected List<String> getCPAIdsImpl()
	{
		log.debug("GetCPAIds");
		return cpaManager.getCPAIds();
	}

	@Override
	public String getCPA(String cpaId) throws CPAServiceException
	{
		try
		{
			return getCPAImpl(cpaId);
		}
		catch (CPAServiceException e)
		{
			log.error("GetCPAId " + cpaId,e);
			throw e;
		}
		catch (Exception e)
		{
			log.error("GetCPAId " + cpaId,e);
			throw new CPAServiceException(e);
		}
	}

	protected String getCPAImpl(String cpaId) throws JAXBException
	{
		log.debug("GetCPAId " + cpaId);
		return JAXBParser.getInstance(CollaborationProtocolAgreement.class).handle(cpaManager.getCPA(cpaId).orElseThrow(CPANotFoundException::new));
	}

	@Override
	public void deleteCache()
	{
		try
		{
			deleteCacheImpl();
		}
		catch (Exception e)
		{
			log.error("DeleteCache",e);
			throw new CPAServiceException(e);
		}
	}

	protected void deleteCacheImpl()
	{
		log.debug("DeleteCache");
		cpaManager.clearCache();
	}
}
