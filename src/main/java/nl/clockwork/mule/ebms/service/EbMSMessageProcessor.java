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
package nl.clockwork.mule.ebms.service;

import java.util.List;

import javax.activation.DataSource;

import nl.clockwork.mule.ebms.model.Signature;
import nl.clockwork.mule.ebms.model.ebxml.AckRequested;
import nl.clockwork.mule.ebms.model.ebxml.Acknowledgment;
import nl.clockwork.mule.ebms.model.ebxml.ErrorList;
import nl.clockwork.mule.ebms.model.ebxml.Manifest;
import nl.clockwork.mule.ebms.model.ebxml.MessageHeader;
import nl.clockwork.mule.ebms.model.ebxml.MessageOrder;
import nl.clockwork.mule.ebms.model.ebxml.StatusRequest;
import nl.clockwork.mule.ebms.model.ebxml.StatusResponse;
import nl.clockwork.mule.ebms.model.ebxml.SyncReply;


public interface EbMSMessageProcessor
{
	//void process(byte[] message, MessageHeader messageHeader, AckRequested ackRequested, Manifest manifest, List<DataSource> attachments);
	void process(byte[] message, MessageHeader messageHeader, SyncReply syncReply, MessageOrder messageOrder, AckRequested ackRequested, Acknowledgment acknowledgment, ErrorList errorList, StatusRequest statusRequest, StatusResponse statusResponse, Manifest manifest, List<DataSource> attachments, Signature signature);
}
