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
package nl.clockwork.ebms.cli.prompt;

import java.io.IOException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import nl.clockwork.ebms.cli.properties.HttpProperties;
import org.jline.prompt.Prompter;
import org.jline.reader.UserInterruptException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HttpSection
{
	public static void prompt(Prompter prompter, HttpProperties properties) throws UserInterruptException, IOException
	{
		properties.setHost(Prompts.input(prompter, "ebmsHost", "EbMS host", properties.getHost()));
		properties.setPort(Prompts.number(prompter, "ebmsPort", "EbMS port", properties.getPort(), 1, 65535));
		properties.setPath(Prompts.input(prompter, "ebmsPath", "EbMS context path", properties.getPath()));
		properties.setChunkedStreamingMode(
				Prompts.confirm(prompter, "chunkedStreamingMode", "Use chunked streaming for message attachments?", properties.isChunkedStreamingMode()));
		properties.setBase64Writer(Prompts.confirm(prompter, "base64Writer", "Use base64 writer for attachments?", properties.isBase64Writer()));
		properties.setSsl(Prompts.confirm(prompter, "ebmsSsl", "Enable SSL?", properties.isSsl()));
		if (properties.isSsl())
			SslSection.prompt(prompter, properties.getSslProperties());
		properties.setProxy(Prompts.confirm(prompter, "useProxy", "Use HTTP proxy?", properties.isProxy()));
		if (properties.isProxy())
			promptProxy(prompter, properties);
	}

	private static void promptProxy(Prompter prompter, HttpProperties properties) throws UserInterruptException, IOException
	{
		val proxy = properties.getProxyProperties();
		proxy.setHost(Prompts.input(prompter, "proxyHost", "Proxy host", proxy.getHost()));
		val port = Prompts.number(prompter, "proxyPort", "Proxy port", proxy.getPort() == null ? 8080 : proxy.getPort(), 1, 65535);
		proxy.setPort(port);
		proxy.setNonProxyHosts(Prompts.input(prompter, "proxyNonProxyHosts", "Non proxy hosts (comma separated)", proxy.getNonProxyHosts()));
		proxy.setUsername(Prompts.input(prompter, "proxyUsername", "Proxy username", proxy.getUsername()));
		proxy.setPassword(Prompts.password(prompter, "proxyPassword", "Proxy password", proxy.getPassword()));
	}
}
