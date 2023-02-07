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
 * Java class for syncReplyMode.type.
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * <p>
 * 
 * <pre>
 * &lt;simpleType name="syncReplyMode.type">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}NMTOKEN">
 *     &lt;enumeration value="mshSignalsOnly"/>
 *     &lt;enumeration value="responseOnly"/>
 *     &lt;enumeration value="signalsAndResponse"/>
 *     &lt;enumeration value="signalsOnly"/>
 *     &lt;enumeration value="none"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 */
@XmlType(name = "syncReplyMode.type")
@XmlEnum
public enum SyncReplyModeType
{

	@XmlEnumValue("mshSignalsOnly")
	MSH_SIGNALS_ONLY("mshSignalsOnly"), @XmlEnumValue("responseOnly")
	RESPONSE_ONLY("responseOnly"), @XmlEnumValue("signalsAndResponse")
	SIGNALS_AND_RESPONSE("signalsAndResponse"), @XmlEnumValue("signalsOnly")
	SIGNALS_ONLY("signalsOnly"), @XmlEnumValue("none")
	NONE("none");

	private final String value;

	SyncReplyModeType(String v)
	{
		value = v;
	}

	public String value()
	{
		return value;
	}

	public static SyncReplyModeType fromValue(String v)
	{
		for (SyncReplyModeType c : SyncReplyModeType.values())
		{
			if (c.value.equals(v))
			{
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}

}
