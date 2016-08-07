package io.bootique.jersey;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.inject.BindingAnnotation;

/**
 * A binding annotation for explicitly registered Jersey resources.
 */
@Target({ ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@BindingAnnotation
public @interface JerseyResource {

}
