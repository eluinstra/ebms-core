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

public class Constants
{
	public static final String EBMS_SOAP_ACTION = "\"ebXML\"";
	public static final String EBMS_VERSION = "2.0";
	public static final String EBMS_DEFAULT_LANGUAGE = "en-US";

	public static final String NSURI_SOAP_ENVELOPE = "http://schemas.xmlsoap.org/soap/envelope/";
	public static final String NSURI_SOAP_NEXT_ACTOR = "http://schemas.xmlsoap.org/soap/actor/next";
	public static final String CID = "cid:";
	
	public static final int MINUTE_IN_MILLIS = 60000;
	
	public static final String MESSAGE_LOG = "nl.clockwork.ebms.message";
}
