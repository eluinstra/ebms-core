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
package nl.clockwork.ebms;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.server.servlet.EbMSServlet;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class EbMSServer
{
	public Server createServer() throws Exception
	{
		var result = new Server();
		var handlerCollection = new ContextHandlerCollection();
		result.setHandler(handlerCollection);
		try (val context = new AnnotationConfigWebApplicationContext())
		{
			context.register(EbMSServerConfig.class);
			val contextLoaderListener = new ContextLoaderListener(context);
			result.addConnector(createConnector(result, "web", 8080));
			handlerCollection.addHandler(webEndpointHandler("web", contextLoaderListener));
			result.addConnector(createConnector(result, "ebms", 8888));
			handlerCollection.addHandler(ebMSEndpointHandler("ebms", contextLoaderListener));
		}
		return result;
	}

	private ServerConnector createConnector(Server server, String name, int port)
	{
		val result = new ServerConnector(server);
		result.setName(name);
		result.setPort(port);
		return result;
	}

	private ServletContextHandler webEndpointHandler(String name, ContextLoaderListener contextLoaderListener)
	{
		val result = new ServletContextHandler();
		result.setVirtualHosts(new String[]{"@" + name});
		result.setContextPath("/");
		result.addServlet(CXFServlet.class, "/service/*");
		result.addEventListener(contextLoaderListener);
		return result;
	}

	private ServletContextHandler ebMSEndpointHandler(String name, ContextLoaderListener contextLoaderListener)
	{
		val result = new ServletContextHandler();
		result.setVirtualHosts(new String[]{"@" + name});
		result.setContextPath("/");
		result.addServlet(EbMSServlet.class, "/ebms");
		result.addEventListener(contextLoaderListener);
		return result;
	}

	public static void main(String[] args) throws Exception
	{
		val server = new EbMSServer().createServer();
		server.start();
		server.join();
	}
}
