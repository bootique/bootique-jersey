package com.nhl.bootique.jersey.unit;

import static org.junit.Assert.assertTrue;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nhl.bootique.BQRuntime;
import com.nhl.bootique.Bootique;
import com.nhl.bootique.command.CommandOutcome;
import com.nhl.bootique.log.BootLogger;
import com.nhl.bootique.log.DefaultBootLogger;

/**
 * A test Jetty/Jersey container to run integration tests against.
 */
public class TestJettyApp {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestJettyApp.class);

	private Consumer<Bootique> configuratior;
	private ExecutorService executor;
	private InMemoryPrintStream stdout;
	private InMemoryPrintStream stderr;

	private BQRuntime runtime;

	public TestJettyApp(Consumer<Bootique> configuratior) {
		this.configuratior = configuratior;
		this.stdout = new InMemoryPrintStream(System.out);
		this.stderr = new InMemoryPrintStream(System.err);
		this.executor = Executors.newCachedThreadPool();
	}

	public void start() {
		this.runtime = init("--server");
		this.executor.submit(() -> run());
	}

	public void startAndWait(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {

		start();

		Future<Boolean> startupFuture = executor.submit(() -> {

			try {
				Server server = runtime.getInstance(Server.class);

				while (!server.isStarted()) {
					LOGGER.info("Server is not started yet, waiting... Current state: " + server.getState());
					Thread.sleep(500);
				}

				return true;
			} catch (Throwable th) {
				LOGGER.warn("Server error", th);
				return false;
			}
		});

		assertTrue(startupFuture.get(timeout, unit));
		LOGGER.info("Server started successfully...");
	}

	public void stop() throws InterruptedException {
		executor.shutdownNow();
		executor.awaitTermination(3, TimeUnit.SECONDS);
	}

	protected BQRuntime init(String... args) {
		Bootique bootique = Bootique.app(args).bootLogger(createBootLogger());
		configuratior.accept(bootique);
		return bootique.createRuntime();
	}

	protected CommandOutcome run() {
		Objects.requireNonNull(runtime);
		try {
			return runtime.getRunner().run();
		} finally {
			runtime.shutdown();
		}
	}

	protected BootLogger createBootLogger() {
		return new DefaultBootLogger(true, stdout, stderr);
	}

	public String getStdout() {
		return stdout.toString();
	}

	public String getStderr() {
		return stderr.toString();
	}
}
