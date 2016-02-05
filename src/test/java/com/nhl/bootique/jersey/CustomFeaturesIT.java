package com.nhl.bootique.jersey;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.junit.Test;

import com.google.inject.Module;
import com.nhl.bootique.config.ConfigurationFactory;
import com.nhl.bootique.jersey.unit.BQJerseyTest;

public class CustomFeaturesIT extends BQJerseyTest {

	@Override
	protected Module createTestModule() {
		return (b) -> {
			JerseyModule.contributeFeatures(b).addBinding().to(Feature1.class);
			JerseyModule.contributeFeatures(b).addBinding().to(Feature2.class);
			b.bind(ConfigurationFactory.class).toInstance(mock(ConfigurationFactory.class));
		};
	}

	@Test
	public void testFeaturesLoaded() {
		assertTrue(Feature1.LOADED);
		assertTrue(Feature2.LOADED);
	}

	static class Feature1 implements Feature {

		static boolean LOADED = false;

		@Override
		public boolean configure(FeatureContext c) {
			LOADED = true;
			return true;
		}
	}

	static class Feature2 implements Feature {

		static boolean LOADED = false;

		@Override
		public boolean configure(FeatureContext c) {
			LOADED = true;
			return true;
		}
	}
}
