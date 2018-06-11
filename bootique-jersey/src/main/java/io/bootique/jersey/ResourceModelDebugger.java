package io.bootique.jersey;

import javax.ws.rs.core.Configuration;

import org.glassfish.jersey.server.model.ModelProcessor;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Debugs all container resources.
 * 
 * @since 0.12
 */
public class ResourceModelDebugger implements ModelProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(ResourceModelDebugger.class);

	@Override
	public ResourceModel processResourceModel(ResourceModel resourceModel, Configuration configuration) {

		if (LOGGER.isDebugEnabled()) {
			resourceModel.getRootResources().forEach(r -> debugResource(r));
		}

		return resourceModel;
	}

	private void debugResource(Resource resource) {
		LOGGER.debug("Resource: " + resource.getPath());
		resource.getChildResources().forEach(r -> debugResource(r));
	}

	@Override
	public ResourceModel processSubResource(ResourceModel subResourceModel, Configuration configuration) {
		return subResourceModel;
	}
}
