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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.security.tls.TlsConfiguration;
import org.mule.transport.tcp.TcpServerSocketFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.InetSocketAddress;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;

public class SslServerSocketFactory extends TcpServerSocketFactory
{
	protected static final Log logger = LogFactory.getLog(SslServerSocketFactory.class);
    private TlsConfiguration tls;

    public SslServerSocketFactory(TlsConfiguration tls)
    {
		logger.info("SSL Patch enabled");
        this.tls = tls;
    }

    @Override
    public ServerSocket createServerSocket(InetAddress address, int port, int backlog, Boolean reuse) throws IOException
    {
        try
        {
            ServerSocketFactory ssf = tls.getServerSocketFactory();
            SSLServerSocket serverSocket = (SSLServerSocket)ssf.createServerSocket();
            //PATCH
			String protocols = System.getProperty("https.protocols");
			if (protocols != null)
			{
				serverSocket.setEnabledProtocols(protocols.split(","));
				logger.info("Enabled SSL Protocols: " + protocols);
			}
			String cipherSuites = System.getProperty("https.cipherSuites");
			if (cipherSuites != null)
			{
				serverSocket.setEnabledCipherSuites(cipherSuites.split(","));
				logger.info("Enabled SSL Cipher Suites: " + cipherSuites);
			}
            return configure(serverSocket, reuse, new InetSocketAddress(address, port), backlog);
        }
        catch (IOException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        }
    }

    @Override
    public ServerSocket createServerSocket(int port, int backlog, Boolean reuse) throws IOException
    {
        try
        {
            ServerSocketFactory ssf = tls.getServerSocketFactory();
            SSLServerSocket serverSocket = (SSLServerSocket)ssf.createServerSocket();
            //PATCH
			String protocols = System.getProperty("https.protocols");
			if (protocols != null)
			{
				serverSocket.setEnabledProtocols(protocols.split(","));
				logger.info("Enabled SSL Protocols:" + protocols);
			}
			String cipherSuites = System.getProperty("https.cipherSuites");
			if (cipherSuites != null)
			{
				serverSocket.setEnabledCipherSuites(cipherSuites.split(","));
				logger.info("Enabled SSL Cipher Suites:" + cipherSuites);
			}
            return configure(serverSocket, reuse, new InetSocketAddress(port), backlog);
        }
        catch (IOException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        }
    }

}
