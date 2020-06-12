package nl.clockwork.ebms.cpa;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
@Getter
public class CPA
{
	CollaborationProtocolAgreement cpa;
	
	public String getId()
	{
		return cpa.getCpaid();
	}
}
