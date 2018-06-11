package io.bootique.jersey;

import java.lang.reflect.Member;

import javax.inject.Singleton;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;

import com.google.inject.ConfigurationException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;

import com.google.inject.internal.Annotations;
import com.google.inject.internal.Errors;
import com.google.inject.internal.ErrorsException;

@Singleton
public class GuiceInjectInjector implements InjectionResolver<Inject>  {

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
