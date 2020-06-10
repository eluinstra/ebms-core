package nl.clockwork.ebms;

import org.apache.xml.security.Init;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.cpa.CPAManager;

@Configuration(proxyBeanMethods = false)
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
}
