/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.bootique.jersey.client.log;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * @deprecated since 1.0.RC1 not used
 */
@Deprecated
public class JULSlf4jLogger extends Logger {

	private static final Consumer<LogRecord> DEFAULT_LOGGER = message -> {
	};

	private Map<Level, Consumer<LogRecord>> loggers;

	public JULSlf4jLogger(String name, org.slf4j.Logger slfLogger) {
		super(name, null);

		// JUL logger should pass through all log levels, so setting its
		// level to ALL...
		setLevel(Level.ALL);

		this.loggers = new HashMap<>();

		Consumer<LogRecord> trace = log -> slfLogger.trace(log.getMessage(), log.getThrown());
		loggers.put(Level.ALL, trace);
		loggers.put(Level.CONFIG, trace);
		loggers.put(Level.FINER, trace);
		loggers.put(Level.FINEST, trace);
		loggers.put(Level.FINE, log -> slfLogger.debug(log.getMessage(), log.getThrown()));

		// a hack: output INFO as DEBUG ... Jersey logging filter uses INFO, and
		// it is way too verbose
		loggers.put(Level.INFO, log -> slfLogger.debug(log.getMessage(), log.getThrown()));

		loggers.put(Level.WARNING, log -> slfLogger.warn(log.getMessage(), log.getThrown()));
		loggers.put(Level.SEVERE, log -> slfLogger.error(log.getMessage(), log.getThrown()));
	}

	@Override
	public void log(LogRecord record) {
		forLevel(record.getLevel()).accept(record);
	}

	private Consumer<LogRecord> forLevel(Level level) {
		return loggers.getOrDefault(level, DEFAULT_LOGGER);
	}

}
