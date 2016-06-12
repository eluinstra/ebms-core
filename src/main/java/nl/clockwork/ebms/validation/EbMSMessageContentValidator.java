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
package nl.clockwork.ebms.validation;

import nl.clockwork.ebms.model.EbMSMessageContent;
import nl.clockwork.ebms.model.Party;

public class EbMSMessageContentValidator
{
	protected EbMSMessageContextValidator messageContextValidator;
	protected MessageOrderValidator messageOrderValidator;
	protected PackageValidator packageValidator;

	public void validateMessage(EbMSMessageContent messageContent) throws ValidatorException
	{
		messageContextValidator.validate(messageContent.getContext());
		messageOrderValidator.generateSequenceNr(messageContent.getContext());
		packageValidator.validate(messageContent);
	}

	public void validatePing(String cpaId, Party fromParty, Party toParty) throws ValidationException
	{
		messageContextValidator.validate(cpaId,fromParty,toParty);
	}

	public void validateMessageStatus(String cpaId, Party fromParty, Party toParty) throws ValidationException
	{
		messageContextValidator.validate(cpaId,fromParty,toParty);
	}

	public void setMessageContextValidator(EbMSMessageContextValidator messageContextValidator)
	{
		this.messageContextValidator = messageContextValidator;
	}

	public void setMessageOrderValidator(MessageOrderValidator messageOrderValidator)
	{
		this.messageOrderValidator = messageOrderValidator;
	}

	public void setPackageValidator(PackageValidator packageValidator)
	{
		this.packageValidator = packageValidator;
	}
}
