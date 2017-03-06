/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ssl;

import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLSocket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.security.tls.TlsConfiguration;
import org.mule.transport.tcp.AbstractTcpSocketFactory;
import org.mule.transport.tcp.TcpSocketKey;

public class SslSocketFactory extends AbstractTcpSocketFactory
{
	protected static final Log logger = LogFactory.getLog(SslSocketFactory.class);
	private TlsConfiguration tls;

	public SslSocketFactory(TlsConfiguration tls)
	{
		this.tls = tls;
	}

	protected Socket createSocket(TcpSocketKey key) throws IOException
	{
		try
		{
			SSLSocket socket = (SSLSocket)tls.getSocketFactory().createSocket(key.getInetAddress(),key.getPort());
			// PATCH
			String protocols = System.getProperty("https.protocols");
			if (protocols != null)
			{
				socket.setEnabledProtocols(protocols.split(","));
				logger.info("Enabled SSL Protocols: " + protocols);
			}
			String cipherSuites = System.getProperty("https.cipherSuites");
			if (cipherSuites != null)
			{
				socket.setEnabledCipherSuites(cipherSuites.split(","));
				logger.info("Enabled SSL Cipher Suites: " + cipherSuites);
			}
			return socket;
		}
		catch (NoSuchAlgorithmException e)
		{
			throw (IOException)new IOException(e.getMessage()).initCause(e);
		}
		catch (KeyManagementException e)
		{
			throw (IOException)new IOException(e.getMessage()).initCause(e);
		}
	}

}
