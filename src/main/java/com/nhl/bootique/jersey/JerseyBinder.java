package com.nhl.bootique.jersey;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

import javax.ws.rs.core.Feature;

import com.google.inject.Binder;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

/**
 * A helper class for downstream modules to contribute things like
 * {@link Feature}'s to Jersey.
 * 
 * @since 0.9
 * @deprecated since 0.11 use {@link JerseyModule#contributeFeatures(Binder)}.
 */
public class JerseyBinder {

	public static JerseyBinder contributeTo(Binder binder) {
		return new JerseyBinder(binder);
	}

	private Binder binder;

	JerseyBinder(Binder binder) {
		this.binder = binder;
	}

	@SafeVarargs
	public final void featureTypes(Class<? extends Feature>... features) {
		Objects.requireNonNull(features);
		featureTypes(Arrays.asList(features));
	}

	public void featureTypes(Collection<Class<? extends Feature>> features) {
		Multibinder<Feature> featureBinder = Multibinder.newSetBinder(binder, Feature.class);
		features.forEach(f -> featureBinder.addBinding().to(f).in(Singleton.class));
	}

	/**
	 * @since 0.11
	 * @param features
	 *            an array of features to register in JAX-RS environment.
	 */
	@SafeVarargs
	public final void features(Feature... features) {
		Objects.requireNonNull(features);
		features(Arrays.asList(features));
	}

	/**
	 * @since 0.11
	 * @param features
	 *            a collection of features to register in JAX-RS environment.
	 */
	public void features(Collection<? extends Feature> features) {
		Multibinder<Feature> featureBinder = Multibinder.newSetBinder(binder, Feature.class);
		features.forEach(f -> featureBinder.addBinding().toInstance(f));
	}
}
