package io.bootique.jersey;

import com.google.inject.Module;
import io.bootique.config.ConfigurationFactory;
import io.bootique.jersey.unit.BQJerseyTest;
import org.junit.Test;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class CustomFeaturesIT extends BQJerseyTest {

	@Override
	protected Module createTestModule() {
		return b -> {
			JerseyModule.extend(b).addFeature(Feature1.class).addFeature(Feature2.class);
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
