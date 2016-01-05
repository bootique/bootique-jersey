package com.nhl.bootique.jersey;

import javax.servlet.Servlet;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import com.nhl.bootique.jetty.MappedServlet;

/**
 * A YAML-configurable factory of Jersey servlet.
 * 
 * @since 0.10
 */
public class JerseyServletFactory {

	protected String servletPath;

	public void setServletPath(String servletPath) {
		this.servletPath = servletPath;
	}

	/**
	 * Conditionally initializes servlet path if it is null.
	 * 
	 * @param config
	 *            a servlet path for the Jersey servlet to use if it was not
	 *            already initialized.
	 */
	public JerseyServletFactory initServletPathIfNotSet(String servletPath) {

		if (this.servletPath == null) {
			this.servletPath = servletPath;
		}

		return this;
	}

	public MappedServlet createJerseyServlet(ResourceConfig resourceConfig) {
		Servlet servlet = new ServletContainer(resourceConfig);
		return new MappedServlet(servlet, servletPath);
	}
}
