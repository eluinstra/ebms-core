package nl.clockwork.ebms.cpa;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.IOUtils;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;

import lombok.val;
import nl.clockwork.ebms.jaxb.JAXBParser;

public class CPATestUtils
{
	public static final String DEFAULT_CPA_ID = "cpaStubEBF.rm.https.signed";

	public static Optional<CollaborationProtocolAgreement> loadDefaultCPA() throws IOException, JAXBException
	{
		return loadCPA(DEFAULT_CPA_ID);
	}

  public static Optional<CollaborationProtocolAgreement> loadCPA(String cpaId) throws IOException, JAXBException
	{
		val s = IOUtils.toString(CPAUtils.class.getResourceAsStream("/nl/clockwork/ebms/cpa/" + cpaId + ".xml"),Charset.forName("UTF-8"));
		return Optional.of(JAXBParser.getInstance(CollaborationProtocolAgreement.class).handleUnsafe(s));
	}
}
