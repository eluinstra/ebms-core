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
package nl.clockwork.mule.ebms.cxf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.phase.Phase;

public class OracleXMLSecSignatureOutInterceptor extends EbMSSecSignatureOutInterceptor
{
	protected transient Log logger = LogFactory.getLog(getClass());

	public OracleXMLSecSignatureOutInterceptor()
	{
		super(Phase.WRITE);
		addBefore(OracleEbMSXMLPrefixFixingOutInterceptor.class.getName());
	}
	
	@Override
	protected Interceptor<?> getEndingInterceptor()
	{
		return new OracleXMLSecSignatureOutEndingInterceptor();
	}

	public class OracleXMLSecSignatureOutEndingInterceptor extends XMLSecSignatureOutEndingInterceptor
	{
		public OracleXMLSecSignatureOutEndingInterceptor()
		{
			super(OracleXMLSecSignatureOutEndingInterceptor.class.getName(),Phase.WRITE_ENDING);
			addAfter(OracleEbMSXMLPrefixFixingOutInterceptor.OracleEbMSXMLPrefixFixingOutEndingInterceptor.class.getName());
		}
	
	}

}
