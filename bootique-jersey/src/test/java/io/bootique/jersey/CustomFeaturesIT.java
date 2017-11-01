package io.bootique.jersey;

import io.bootique.jetty.test.junit.JettyTestFactory;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import static org.junit.Assert.assertTrue;

public class CustomFeaturesIT {

	@Rule
	public JettyTestFactory testFactory = new JettyTestFactory();

	@Test
	public void testFeaturesLoaded() {

		testFactory.app().autoLoadModules()
				.module(b -> JerseyModule.extend(b).addFeature(Feature1.class).addFeature(Feature2.class))
				.start();


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
