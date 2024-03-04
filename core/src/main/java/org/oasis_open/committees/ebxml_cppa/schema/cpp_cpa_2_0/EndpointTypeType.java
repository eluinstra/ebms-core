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
package org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for endpointType.type.
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * <p>
 * 
 * <pre>
 * &lt;simpleType name="endpointType.type">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}NMTOKEN">
 *     &lt;enumeration value="login"/>
 *     &lt;enumeration value="request"/>
 *     &lt;enumeration value="response"/>
 *     &lt;enumeration value="error"/>
 *     &lt;enumeration value="allPurpose"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 */
@XmlType(name = "endpointType.type")
@XmlEnum
public enum EndpointTypeType
{

	@XmlEnumValue("login")
	LOGIN("login"), @XmlEnumValue("request")
	REQUEST("request"), @XmlEnumValue("response")
	RESPONSE("response"), @XmlEnumValue("error")
	ERROR("error"), @XmlEnumValue("allPurpose")
	ALL_PURPOSE("allPurpose");

	private final String value;

	EndpointTypeType(String v)
	{
		value = v;
	}

	public String value()
	{
		return value;
	}

	public static EndpointTypeType fromValue(String v)
	{
		for (EndpointTypeType c : EndpointTypeType.values())
		{
			if (c.value.equals(v))
			{
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}

}
