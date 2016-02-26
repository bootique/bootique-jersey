package com.nhl.bootique.jersey;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;

import org.junit.Test;

import com.google.inject.Module;
import com.nhl.bootique.config.ConfigurationFactory;
import com.nhl.bootique.jersey.unit.BQJerseyTest;

public class CustomDynamicFeaturesIT extends BQJerseyTest {

	@Override
	protected Module createTestModule() {
		return (b) -> {
			JerseyModule.contributeDynamicFeatures(b).addBinding().to(DynamicFeature1.class);
			JerseyModule.contributeDynamicFeatures(b).addBinding().to(DynamicFeature2.class);
			b.bind(ConfigurationFactory.class).toInstance(mock(ConfigurationFactory.class));
		};
	}

	@Test
	public void testFeaturesLoaded() {
		assertTrue(DynamicFeature1.LOADED);
		assertTrue(DynamicFeature2.LOADED);
	}

	static class DynamicFeature1 implements DynamicFeature {

		static boolean LOADED = false;

		@Override
		public void configure(ResourceInfo resourceInfo, FeatureContext context) {
			LOADED = true;
		}
	}

	static class DynamicFeature2 implements DynamicFeature {

		static boolean LOADED = false;

		@Override
		public void configure(ResourceInfo resourceInfo, FeatureContext context) {
			LOADED = true;
		}
	}

}
