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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import nl.clockwork.ebms.cli.properties.KeystoreProperties;
import nl.clockwork.ebms.cli.properties.SslProperties;
import nl.clockwork.ebms.common.security.KeyStoreType;
import org.apache.commons.lang3.StringUtils;
import org.jline.prompt.Prompter;
import org.jline.reader.UserInterruptException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SslSection
{
	public static void prompt(Prompter prompter, SslProperties properties) throws UserInterruptException, IOException
	{
		properties.setEnabledProtocols(
				splitList(Prompts.input(prompter, "sslProtocols", "TLS protocols (comma separated, empty for defaults)", properties.getProtocols())));
		properties.setEnabledCipherSuites(
				splitList(Prompts.input(prompter, "sslCipherSuites", "TLS cipher suites (comma separated, empty for defaults)", properties.getCipherSuites())));
		properties.setRequireClientAuthentication(
				Prompts.confirm(prompter, "sslRequireClientAuth", "Require client authentication?", properties.isRequireClientAuthentication()));
		properties.setVerifyHostnames(Prompts.confirm(prompter, "sslVerifyHostnames", "Verify hostnames?", properties.isVerifyHostnames()));
		keystore(prompter, properties.getKeystoreProperties(), "keystore", "Server keystore");
		keystore(prompter, properties.getClientKeystoreProperties(), "clientKeystore", "Client keystore");
		keystore(prompter, properties.getTruststoreProperties(), "truststore", "Truststore");
	}

	public static void keystore(Prompter prompter, KeystoreProperties properties, String prefix, String label) throws UserInterruptException, IOException
	{
		val selected = Prompts.list(prompter, prefix + "Type", label + " type", keyStoreTypeOptions(), properties.getType().name());
		properties.setType(KeyStoreType.valueOf(selected));
		properties.setUri(Prompts.input(prompter, prefix + "Path", label + " path (file or URL)", properties.getUri()));
		properties.setPassword(Prompts.password(prompter, prefix + "Password", label + " password", properties.getPassword()));
		properties.setDefaultAlias(Prompts.input(prompter, prefix + "DefaultAlias", label + " default alias (empty for first)", properties.getDefaultAlias()));
	}

	private static List<String> splitList(String value)
	{
		val result = new java.util.ArrayList<String>();
		if (StringUtils.isNotBlank(value))
			result.addAll(Arrays.asList(value.split(",")));
		return result.stream().map(String::trim).toList();
	}

	private static Map<String, String> keyStoreTypeOptions()
	{
		val result = new LinkedHashMap<String, String>();
		Arrays.asList(KeyStoreType.values()).forEach(t -> result.put(t.name(), t.name()));
		return result;
	}
}
