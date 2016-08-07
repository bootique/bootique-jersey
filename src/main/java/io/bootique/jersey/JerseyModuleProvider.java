package io.bootique.jersey;

import com.google.inject.Module;
import io.bootique.BQModuleProvider;

public class JerseyModuleProvider implements BQModuleProvider {

	@Override
	public Module module() {
		return new JerseyModule();
	}
}
