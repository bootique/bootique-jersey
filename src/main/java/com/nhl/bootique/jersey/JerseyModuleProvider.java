package com.nhl.bootique.jersey;

import com.google.inject.Module;
import com.nhl.bootique.BQModuleProvider;

public class JerseyModuleProvider implements BQModuleProvider {

	@Override
	public Module module() {
		return JerseyModule.builder().build();
	}
}
