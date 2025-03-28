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
package nl.clockwork.ebms.cpa;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.validation.CPAValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CPAServiceConfig
{
	@Bean
	public CPAValidator cpaValidator(CPAManager cpaManager)
	{
		return new CPAValidator(cpaManager);
	}

	@Bean
	public CPAService cpaService(CPAManager cpaManager, CPAValidator cpaValidator)
	{
		return new CPAServiceImpl(cpaManager, cpaValidator);
	}

	@Bean
	public CPARestService cpaRestService(CPAService cpaService)
	{
		return new CPARestService((CPAServiceImpl)cpaService);
	}
}
