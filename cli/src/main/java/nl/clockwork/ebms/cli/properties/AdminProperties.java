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
package nl.clockwork.ebms.cli.properties;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Model of an ebms-admin properties file. All sections are present in every file; which sections are written depends on the {@link PropertiesType}.
 */
@Data
@NoArgsConstructor
public class AdminProperties
{
	@NonNull
	ConsoleProperties consoleProperties = new ConsoleProperties();
	@NonNull
	CoreProperties coreProperties = new CoreProperties();
	@NonNull
	ServiceProperties serviceProperties = new ServiceProperties();
	@NonNull
	HttpProperties httpProperties = new HttpProperties();
	@NonNull
	ServerDatabase serverDatabase = new ServerDatabase();
	@NonNull
	JdbcProperties jdbcProperties = new JdbcProperties();
	@NonNull
	SignatureProperties signatureProperties = new SignatureProperties();
	@NonNull
	EncryptionProperties encryptionProperties = new EncryptionProperties();
	@NonNull
	Map<String, String> additionalProperties = new LinkedHashMap<>();
}
