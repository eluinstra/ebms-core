/*
 * Copyright 2011 - 2026 Clockwork
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
package nl.clockwork.ebms.server.endpoint.servlet.filters;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Helpers to decide whether a connector host is reachable only from the local machine, and whether a host points at a non-public (private / loopback /
 * link-local / site-local) network address. Used to keep an unauthenticated management API off non-loopback binds (F1) and to reject CPA endpoint URIs that
 * would redirect traffic to internal addresses (F3).
 */
public final class LoopbackUtils
{
	private LoopbackUtils()
	{
	}

	/**
	 * Returns true when the given host can only be reached from the local machine (an empty / null bind, the literal name "localhost", or a host that resolves to
	 * a loopback address).
	 */
	public static boolean isLoopback(String host)
	{
		if (host == null || host.trim().isEmpty() || host.trim().equalsIgnoreCase("localhost"))
			return true;
		try
		{
			return InetAddress.getByName(host.trim()).isLoopbackAddress();
		}
		catch (UnknownHostException e)
		{
			// A name that cannot be resolved is certainly not a local loopback interface; treat it as
			// non-loopback so the caller applies the stricter (authenticated) path.
			return false;
		}
	}

	/**
	 * Returns true when any address the host resolves to is a non-public network address (loopback, link-local, site-local / RFC-1918, any-local or multicast).
	 * Used to block CPA endpoints that target internal addresses.
	 */
	public static boolean isPrivateOrLocalHost(String host)
	{
		try
		{
			for (InetAddress address : InetAddress.getAllByName(host))
			{
				if (address.isAnyLocalAddress()
						|| address.isLoopbackAddress()
						|| address.isLinkLocalAddress()
						|| address.isSiteLocalAddress()
						|| address.isMulticastAddress())
					return true;
			}
			return false;
		}
		catch (UnknownHostException e)
		{
			// A name that cannot be resolved is not a usable public endpoint; fail closed.
			return true;
		}
	}
}
