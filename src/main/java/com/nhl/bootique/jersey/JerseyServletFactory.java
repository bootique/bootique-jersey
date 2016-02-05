package com.nhl.bootique.jersey;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import javax.servlet.Servlet;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nhl.bootique.jetty.MappedServlet;

/**
 * A YAML-configurable factory of Jersey servlet.
 * 
 * @since 0.10
 */
public class JerseyServletFactory {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(JerseyServletFactory.class);

	protected String urlPattern;

	/**
	 * @deprecated since 0.11 in favor of {@link #setUrlPattern(String)}.
	 * @param servletPath
	 *            a URL: pattern for the Jersey servlet. Default is "/*".
	 */
	public void setServletPath(String servletPath) {
		LOGGER.warn("Using deprecated 'servletPath' property; use 'urlPattern' instead."); 
		setUrlPattern(urlPattern);
	}

	/**
	 * @since 0.11
	 * @param urlPattern
	 *            a URL: pattern for the Jersey servlet. Default is "/*".
	 */
	public void setUrlPattern(String urlPattern) {
		this.urlPattern = urlPattern;
	}

	/**
	 * Conditionally initializes servlet path if it is null.
	 * 
	 * @param servletPath
	 *            a servlet path for the Jersey servlet to use if it was not
	 *            already initialized.
	 * @return self.
	 * @deprecated since 0.11 in favor of {@link #initUrlPatternIfNotSet(String)}.
	 */
	public JerseyServletFactory initServletPathIfNotSet(String servletPath) {
		return initUrlPatternIfNotSet(servletPath);
	}

	/**
	 * Conditionally initializes servlet url pattern if it is null.
	 * 
	 * @param urlPattern
	 *            a URL: pattern for the Jersey servlet unless it was already
	 *            set.
	 * @return self.
	 * @since 0.11
	 */
	public JerseyServletFactory initUrlPatternIfNotSet(String urlPattern) {

		if (this.urlPattern == null) {
			this.urlPattern = urlPattern;
		}

		return this;
	}

	public MappedServlet createJerseyServlet(ResourceConfig resourceConfig) {
		Servlet servlet = new ServletContainer(resourceConfig);
		Set<String> urlPatterns = Collections.singleton(Objects.requireNonNull(urlPattern));

		return new MappedServlet(servlet, urlPatterns);
	}
}
