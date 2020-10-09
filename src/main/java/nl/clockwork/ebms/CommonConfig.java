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
package nl.clockwork.ebms;

import org.apache.xml.security.Init;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.util.LoggingUtils;
import nl.clockwork.ebms.util.LoggingUtils.Status;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommonConfig
{
	@Autowired
	CPAManager cpaManager;
	@Value("${ebmsMessage.attachment.outputDirectory}")
	String attachmentOutputDirectory;
	@Value("${ebmsMessage.attachment.memoryTreshold}")
	int attachmentMemoryTreshold;
	@Value("${ebmsMessage.attachment.cipherTransformation}")
	String attachmentCipherTransformation;
	@Value("${logging.mdc}")
	Status mdc;

	@Bean
	public void initXMLSecurity()
	{
		Init.init();
	}

	@Bean
	public EbMSMessageFactory ebMSMessageFactory()
	{
		return new EbMSMessageFactory(cpaManager,ebMSIdGenerator());
	}

	@Bean
	public void EbMSAttachmentFactory()
	{
		EbMSAttachmentFactory.init(attachmentOutputDirectory,attachmentMemoryTreshold,attachmentCipherTransformation);
	}

	@Bean
	public EbMSIdGenerator ebMSIdGenerator()
	{
		return new EbMSIdGenerator();
	}

	@Bean
	public void configureLoggingUtils()
	{
		LoggingUtils.mdc = mdc;
	}
}
