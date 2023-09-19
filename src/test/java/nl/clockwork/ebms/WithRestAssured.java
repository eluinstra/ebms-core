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
package nl.clockwork.ebms;

import io.restassured.internal.multipart.MultiPartSpecificationImpl;
import io.restassured.specification.MultiPartSpecification;
import java.util.Map;

public interface WithRestAssured
{

	default MultiPartSpecification createEbMSPart(String content)
	{
		var result = new MultiPartSpecificationImpl();
		result.setHeaders(Map.of());
		result.setMimeType("text/xml");
		result.setContent(content);
		return result;
	}

	default MultiPartSpecification createAfleverberichtAttachment(String cid, String content)
	{
		var result = new MultiPartSpecificationImpl();
		result.setHeaders(Map.of());
		result.setControlName(cid);
		result.setMimeType("text/xml");
		result.setContent(content);
		return result;
	}

}
