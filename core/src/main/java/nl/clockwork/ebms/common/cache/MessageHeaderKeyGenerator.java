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
package nl.clockwork.ebms.common.cache;

import java.lang.reflect.Method;
import java.util.ArrayList;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.lang.NonNull;

public class MessageHeaderKeyGenerator implements KeyGenerator
{
	@Override
	@NonNull
	public Object generate(@NonNull Object target, @NonNull Method method, @NonNull Object...params)
	{
		val messageHeader = (MessageHeader)params[0];
		val values = new ArrayList<>();
		values.add(messageHeader.getCPAId());
		values.add(messageHeader.getTo().getPartyId());
		values.add(messageHeader.getFrom().getPartyId());
		values.add(messageHeader.getService());
		values.add(messageHeader.getAction());
		return method.getName() + "[" + StringUtils.join(values, ",") + "]";
	}
}
