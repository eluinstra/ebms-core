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
import nl.clockwork.ebms.task.SendTaskStatus;

public class SendTaskStatusType extends AbstractType<SendTaskStatus>
{
	public SendTaskStatusType(int type)
	{
		super(type);
	}

	@Override
	public Class<SendTaskStatus> getReturnedClass()
	{
		return SendTaskStatus.class;
	}

	@Override
	public SendTaskStatus getValue(ResultSet rs, int startIndex) throws SQLException
	{
		val id = rs.getObject(startIndex,Integer.class);
		return id != null ? SendTaskStatus.get(id).orElseThrow(() -> new IllegalArgumentException("SendTaskStatus " + id + " is not valid!")) : null;
	}

	@Override
	public void setValue(PreparedStatement st, int startIndex, SendTaskStatus value) throws SQLException
	{
		st.setInt(startIndex,value != null ? value.getId() : null);
	}
}
