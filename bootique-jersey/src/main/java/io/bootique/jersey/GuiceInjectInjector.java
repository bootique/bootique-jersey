/**
 *    Licensed to the ObjectStyle LLC under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ObjectStyle LLC licenses
 *  this file to you under the Apache License, Version 2.0 (the
 *  “License”); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.bootique.jersey;

import java.lang.reflect.Member;

import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;

import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;

import com.google.inject.ConfigurationException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.internal.Annotations;
import com.google.inject.internal.Errors;
import com.google.inject.internal.ErrorsException;

@Singleton
public class GuiceInjectInjector implements InjectionResolver<Inject> {

	private Injector injector;

	public GuiceInjectInjector(@Context Configuration configuration) {
		this.injector = GuiceBridgeFeature.getInjector(configuration);
	}

	@Override
	public boolean isConstructorParameterIndicator() {
		return true;
	}

	@Override
	public boolean isMethodParameterIndicator() {
		return false;
	}

	@Override
	public Object resolve(Injectee injectee, ServiceHandle<?> serviceHandle) {

		if (injectee.getRequiredType() instanceof Class) {

			TypeLiteral<?> typeLiteral = TypeLiteral.get(injectee.getRequiredType());
			Errors errors = new Errors(injectee.getParent());
			Key<?> key;
			try {
				key = Annotations.getKey(typeLiteral, (Member) injectee.getParent(),
						injectee.getParent().getDeclaredAnnotations(), errors);
			} catch (ErrorsException e) {
				errors.merge(e.getErrors());
				throw new ConfigurationException(errors.getMessages());
			}

			return injector.getInstance(key);
		}

		throw new IllegalStateException("Can't process injection point: " + injectee.getRequiredType());
	}

}
