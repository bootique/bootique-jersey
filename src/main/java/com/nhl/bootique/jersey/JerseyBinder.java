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
	public final void features(Class<? extends Feature>... features) {
		Objects.requireNonNull(features);
		features(Arrays.asList(features));
	}

	public void features(Collection<Class<? extends Feature>> features) {
		Multibinder<Feature> featureBinder = Multibinder.newSetBinder(binder, Feature.class);
		features.forEach(f -> featureBinder.addBinding().to(f).in(Singleton.class));
	}
}
