package io.bootique.jersey.jakarta;

import io.bootique.BootiqueException;
import io.bootique.di.Injector;
import io.bootique.di.Key;
import io.bootique.di.TypeLiteral;
import org.glassfish.hk2.api.Injectee;

import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Base utilities for bridge between Bootique DI and HK2 container
 */
abstract class BaseBqHk2Bridge {

    protected final Injector injector;

    protected BaseBqHk2Bridge(Injector injector) {
        this.injector = injector;
    }

    protected Provider<?> resolveBqProvider(Injectee injectee) {
        TypeLiteral<?> typeLiteral = TypeLiteral.of(injectee.getRequiredType());
        if(javax.inject.Provider.class.equals(typeLiteral.getRawType())) {
            Type requiredType = getGenericParameterType(injectee.getRequiredType());
            if(requiredType == null) {
                throw new BootiqueException(-1, "Not specified generic type for Provider injection point.");
            }
            typeLiteral = TypeLiteral.of(requiredType);
        }

        Annotation bindingAnnotation = injectee.getRequiredQualifiers().isEmpty()
                ? null
                : injectee.getRequiredQualifiers().iterator().next();
        Key<?> key = bindingAnnotation == null
                ? Key.get(typeLiteral)
                : Key.get(typeLiteral, bindingAnnotation);

        if(!injector.hasProvider(key) && !allowDynamicInjectionForKey(injectee, key)) {
            return null;
        }

        return injector.getProvider(key);
    }

    /**
     * Check we can create service for a given key dynamically (i.e. without explicit binding in the injector).
     * This method could be overridden in children.
     */
    protected boolean allowDynamicInjectionForKey(Injectee injectionPoint, Key<?> key) {
        return true;
    }

    static Type getGenericParameterType(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type[] parameters = parameterizedType.getActualTypeArguments();

            if (parameters.length == 1) {
                return parameters[0];
            }
        }

        return null;
    }
}
