/**
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
package nl.clockwork.ebms.querydsl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.querydsl.sql.types.AbstractType;

import lombok.val;
import nl.clockwork.ebms.event.processor.EbMSEventStatus;

public class EbMSEventStatusType extends AbstractType<EbMSEventStatus>
{
	public EbMSEventStatusType(int type)
	{
		super(type);
	}

	@Override
	public Class<EbMSEventStatus> getReturnedClass()
	{
		return EbMSEventStatus.class;
	}

	@Override
	public EbMSEventStatus getValue(ResultSet rs, int startIndex) throws SQLException
	{
		val id = rs.getObject(startIndex,Integer.class);
		return id != null ? EbMSEventStatus.get(id).orElseThrow(() -> new IllegalArgumentException("EbMSEventStatus " + id + " is not valid!")) : null;
	}

	@Override
	public void setValue(PreparedStatement st, int startIndex, EbMSEventStatus value) throws SQLException
	{
		st.setInt(startIndex,value != null ? value.getId() : null);
	}
}
