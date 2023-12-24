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
 * Java class for persistenceLevel.type.
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * <p>
 * 
 * <pre>
 * &lt;simpleType name="persistenceLevel.type">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}Name">
 *     &lt;enumeration value="none"/>
 *     &lt;enumeration value="transient"/>
 *     &lt;enumeration value="persistent"/>
 *     &lt;enumeration value="transient-and-persistent"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 */
@XmlType(name = "persistenceLevel.type")
@XmlEnum
public enum PersistenceLevelType
{

	@XmlEnumValue("none")
	NONE("none"), @XmlEnumValue("transient")
	TRANSIENT("transient"), @XmlEnumValue("persistent")
	PERSISTENT("persistent"), @XmlEnumValue("transient-and-persistent")
	TRANSIENT_AND_PERSISTENT("transient-and-persistent");

	private final String value;

	PersistenceLevelType(String v)
	{
		value = v;
	}

	public String value()
	{
		return value;
	}

	public static PersistenceLevelType fromValue(String v)
	{
		for (PersistenceLevelType c : PersistenceLevelType.values())
		{
			if (c.value.equals(v))
			{
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}

}
