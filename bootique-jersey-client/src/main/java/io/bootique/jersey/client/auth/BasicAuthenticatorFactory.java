/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.bootique.jersey.client.auth;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.di.Injector;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Objects;

/**
 * @deprecated The users are encouraged to switch to the Jakarta-based flavor
 */
@Deprecated(since = "3.0", forRemoval = true)
@JsonTypeName("basic")
@BQConfig("Authenticator for BASIC auth protocol.")
public class BasicAuthenticatorFactory implements AuthenticatorFactory {

	private String username;
	private String password;

	public String getUsername() {
		return username;
	}

	@BQConfigProperty("A user name of the BASIC auth credentials used to access the remote resource.")
	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	@BQConfigProperty("A password of the BASIC auth credentials used to access the remote resource.")
	public void setPassword(String password) {
		this.password = password;
	}
	
	@Override
	public ClientRequestFilter createAuthFilter(Injector injector) {
		Objects.requireNonNull(username, "BASIC 'username' is null");
		Objects.requireNonNull(password, "BASIC 'password' is null");

		return new BasicAuthenticator(username, password);
	}

	static class BasicAuthenticator implements ClientRequestFilter {

		private String basicAuth;

		public BasicAuthenticator(String username, String password) {
			this.basicAuth = createBasicAuth(username, password);
		}

		public void filter(ClientRequestContext requestContext) throws IOException {
			MultivaluedMap<String, Object> headers = requestContext.getHeaders();
			headers.add("Authorization", basicAuth);
		}

		static String createBasicAuth(String username, String password) {
			String token = username + ":" + password;
			try {
				return "Basic " + DatatypeConverter.printBase64Binary(token.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException ex) {
				throw new IllegalStateException("Cannot encode with UTF-8", ex);
			}
		}
	}
}
