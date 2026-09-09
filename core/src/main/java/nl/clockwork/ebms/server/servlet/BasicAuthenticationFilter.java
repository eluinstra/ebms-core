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
package nl.clockwork.ebms.server.servlet;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Servlet filter that restricts a servlet context to authenticated Basic users read from a
 * Jetty-style {@code realm.properties} file ({@code user=password,role1,role2}).
 *
 * <p>Passwords must be stored as a salted, iterated PBKDF2-HMAC-SHA256 hash in the form
 * {@code {PBKDF2WithHmacSHA256}<base64(salt|iterations(4)|derivedKey)>}. Legacy weak formats
 * ({@code MD5:}, {@code OBF:}, {@code CRYPT:} and plaintext) are rejected, so a realm containing
 * them will not authenticate. Use {@link #main(String[])} to generate a password hash for the realm.
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BasicAuthenticationFilter implements Filter
{
	private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
	private static final String PBKDF2_PREFIX = "{" + PBKDF2_ALGORITHM + "}";
	private static final int ITERATIONS = 210000;
	private static final int SALT_BYTES = 16;
	private static final int KEY_BYTES = 32;

	String realm;
	Map<String, String> users;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
		try
		{
			realm = filterConfig.getInitParameter("realm");
			val realmFile = new File(filterConfig.getInitParameter("realmFile"));
			val lines = FileUtils.readLines(realmFile, Charset.defaultCharset());
			users = lines.stream()
					.map(s -> StringUtils.split(s, ","))
					.filter(a -> a.length == 2 && "user".equals(a[1]))
					.map(a -> StringUtils.split(a[0], ":"))
					.filter(u -> u.length == 2)
					.collect(Collectors.toMap(u -> u[0], u -> u[1]));
		}
		catch (IOException e)
		{
			throw new ServletException(e);
		}
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		val authorization = ((HttpServletRequest)request).getHeader("Authorization");
		if (validate(users, authorization))
			chain.doFilter(request, response);
		else
		{
			((HttpServletResponse)response).setHeader("WWW-Authenticate", "Basic realm=\"" + realm + "\"");
			((HttpServletResponse)response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		}
	}

	private boolean validate(Map<String, String> users, String authorization)
	{
		if (authorization == null || !authorization.toLowerCase().startsWith("basic"))
			return false;
		val credentials = new String(Base64.getDecoder().decode(authorization.substring("basic".length()).trim()), Charset.defaultCharset());
		val parts = StringUtils.split(credentials, ":");
		if (parts.length != 2)
			return false;
		val stored = users.get(parts[0]);
		return stored != null && checkPassword(stored, parts[1]);
	}

	/**
	 * Compares a supplied password against a stored {@code {PBKDF2WithHmacSHA256}...} hash.
	 *
	 * @return true on a match; false on a mismatch or when the stored value is in an unsupported
	 *         (legacy/weak) format
	 */
	boolean checkPassword(String storedHash, String password)
	{
		if (!storedHash.startsWith(PBKDF2_PREFIX))
			return false;
		try
		{
			val raw = Base64.getDecoder().decode(storedHash.substring(PBKDF2_PREFIX.length()));
			val saltLength = raw.length - KEY_BYTES - 4;
			val salt = Arrays.copyOfRange(raw, 0, saltLength);
			val iterations = ((raw[saltLength] & 0xff) << 24) | ((raw[saltLength + 1] & 0xff) << 16) | ((raw[saltLength + 2] & 0xff) << 8) | (raw[saltLength + 3] & 0xff);
			val expected = Arrays.copyOfRange(raw, saltLength + 4, raw.length);
			return MessageDigest.isEqual(derive(password.toCharArray(), salt, iterations), expected);
		}
		catch (GeneralSecurityException e)
		{
			return false;
		}
	}

	private byte[] derive(char[] password, byte[] salt, int iterations) throws NoSuchAlgorithmException, InvalidKeySpecException
	{
		val spec = new PBEKeySpec(password, salt, iterations, KEY_BYTES * Byte.SIZE);
		return SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).getEncoded();
	}

	/**
	 * Generates a new {@code {PBKDF2WithHmacSHA256}...} hash. Runs standalone:
	 * {@code java ... BasicAuthenticationFilter <password>} prints the value to store in the realm file.
	 */
	public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeySpecException
	{
		if (args.length != 1)
		{
			System.err.println("usage: BasicAuthenticationFilter <password>");
			System.exit(1);
			return;
		}
		val salt = new byte[SALT_BYTES];
		new SecureRandom().nextBytes(salt);
		val key = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(new PBEKeySpec(args[0].toCharArray(), salt, ITERATIONS, KEY_BYTES * Byte.SIZE)).getEncoded();
		val encoded = new byte[salt.length + 4 + key.length];
		System.arraycopy(salt, 0, encoded, 0, salt.length);
		int offset = salt.length;
		encoded[offset++] = (byte)(ITERATIONS >>> 24);
		encoded[offset++] = (byte)(ITERATIONS >>> 16);
		encoded[offset++] = (byte)(ITERATIONS >>> 8);
		encoded[offset++] = (byte)ITERATIONS;
		System.arraycopy(key, 0, encoded, offset, key.length);
		System.out.println(PBKDF2_PREFIX + Base64.getEncoder().encodeToString(encoded));
	}

	@Override
	public void destroy()
	{
		// do nothing
	}

}
