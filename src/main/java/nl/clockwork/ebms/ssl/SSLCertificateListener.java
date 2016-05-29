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
package nl.clockwork.ebms.ssl;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLPeerUnverifiedException;

import nl.clockwork.ebms.validation.SSLSessionManager;

public class SSLCertificateListener implements HandshakeCompletedListener
{
	public void handshakeCompleted(HandshakeCompletedEvent event)
	{
		nl.clockwork.ebms.model.SSLSession sslSession = new nl.clockwork.ebms.model.SSLSession();
		sslSession.setCipherSuite(event.getSession().getCipherSuite());
		sslSession.setProtocol(event.getSession().getProtocol());
		sslSession.setLocalCertificates(event.getSession().getLocalCertificates());
		sslSession.setPeerHost(event.getSession().getPeerHost());
		sslSession.setPeerPort(event.getSession().getPeerPort());
		try
		{
			sslSession.setPeerCertificates(event.getSession().getPeerCertificates());
			sslSession.setPeerCertificateChain(event.getSession().getPeerCertificateChain());
		}
		catch (SSLPeerUnverifiedException e)
		{
		}
		SSLSessionManager.setSSLSession(sslSession);
	}
}
