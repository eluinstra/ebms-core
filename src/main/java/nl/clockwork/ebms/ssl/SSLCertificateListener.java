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
