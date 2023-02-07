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


import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for messageOrderSemantics.type.
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * <p>
 * 
 * <pre>
 * &lt;simpleType name="messageOrderSemantics.type">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}Name">
 *     &lt;enumeration value="Guaranteed"/>
 *     &lt;enumeration value="NotGuaranteed"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 */
@XmlType(name = "messageOrderSemantics.type")
@XmlEnum
public enum MessageOrderSemanticsType
{

	@XmlEnumValue("Guaranteed")
	GUARANTEED("Guaranteed"), @XmlEnumValue("NotGuaranteed")
	NOT_GUARANTEED("NotGuaranteed");

	private final String value;

	MessageOrderSemanticsType(String v)
	{
		value = v;
	}

	public String value()
	{
		return value;
	}

	public static MessageOrderSemanticsType fromValue(String v)
	{
		for (MessageOrderSemanticsType c : MessageOrderSemanticsType.values())
		{
			if (c.value.equals(v))
			{
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}

}
