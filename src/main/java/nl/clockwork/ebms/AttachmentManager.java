/*******************************************************************************
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
 ******************************************************************************/
package nl.clockwork.ebms;

import java.util.Collections;
import java.util.List;

import nl.clockwork.ebms.model.EbMSAttachment;

public class AttachmentManager
{
	private static ThreadLocal<List<EbMSAttachment>> attachments = new ThreadLocal<List<EbMSAttachment>>()
	{
		protected synchronized List<EbMSAttachment> initialValue()
		{
			return Collections.emptyList();
		}
	};

	public static void set(List<EbMSAttachment> attachments)
	{
		AttachmentManager.attachments.set(attachments);
	}

	public static List<EbMSAttachment> get()
	{
		return attachments.get();
	}

}
