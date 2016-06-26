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
package nl.clockwork.ebms.dao.hsqldb;

import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.dao.AbstractEbMSDAO;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.model.EbMSMessageContext;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

public class EbMSDAOImpl extends AbstractEbMSDAO
{
	public EbMSDAOImpl(TransactionTemplate transactionTemplate, JdbcTemplate jdbcTemplate)
	{
		super(transactionTemplate,jdbcTemplate);
	}

	@Override
	public EbMSMessageContext getLastReceivedMessage(String cpaId, String conversationId) throws DAOException
	{
		try
		{
			JdbcTemplate result = jdbcTemplate;
			return result.queryForObject(
				EbMSMessageContextRowMapper.getBaseQuery() +
				" where cpa_id = ?" +
				" and conversation_id = ?" +
				" and message_nr = 0" +
				" and status < 10" +
				" order by sequence_nr desc, time_stamp asc" +
				" limit 1",
				new EbMSMessageContextRowMapper(),
				cpaId,
				conversationId
			);
		}
		catch(EmptyResultDataAccessException e)
		{
			return null;
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public String getMessageIdsQuery(String messageContextFilter, EbMSMessageStatus status, int maxNr)
	{
		return "select message_id" +
		" from ebms_message" +
		" where message_nr = 0" + 
		" and status = " + status.id() +
		messageContextFilter +
		" order by time_stamp asc" +
		" limit " + maxNr;
	}

}
