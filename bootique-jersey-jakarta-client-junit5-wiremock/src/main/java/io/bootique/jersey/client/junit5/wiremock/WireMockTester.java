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
package io.bootique.jersey.client.junit5.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.bootique.BQCoreModule;
import io.bootique.di.BQModule;
import io.bootique.junit5.BQTestScope;
import io.bootique.junit5.scope.BQAfterScopeCallback;
import io.bootique.junit5.scope.BQBeforeScopeCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Bootique test tool that sets up and manages a WireMock "server". Each tester should be annotated with
 * {@link io.bootique.junit5.BQTestTool}.
 *
 * @since 3.0
 */
public abstract class WireMockTester<T extends WireMockTester<T>> implements BQBeforeScopeCallback, BQAfterScopeCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(WireMockTester.class);

    protected boolean verbose;
    protected WireMockConfiguration config;
    protected volatile WireMockServer wiremockServer;

    /**
     * Creates a new mock tester with a real backend at the given URL.
     *
     * @param targetUrl url of resource to mock, for example: "https://www.example.com/foo/bar"
     */
    public static WireMockRecordingTester recordingTester(String targetUrl) {
        return new WireMockRecordingTester(WireMockUrlParts.of(targetUrl));
    }

    /**
     * Creates a new mock tester whose behavior should be later configured with custom stubs.
     */
    public static WireMockStubbingTester stubbingTester() {
        return new WireMockStubbingTester();
    }

    // keep for a while to allow early adopters to upgrade
    @Deprecated(since = "3.0.M2")
    public static WireMockRecordingTester create(String targetUrl) {
        return recordingTester(targetUrl);
    }

    // keep for a while to allow early adopters to upgrade
    @Deprecated(since = "3.0.M2")
    public static WireMockRecordingTester tester(String targetUrl) {
        return recordingTester(targetUrl);
    }

    /**
     * A builder method that sets an optional custom config for WireMockServer. Overrides certain customization made to the
     * tester.
     */
    public T config(WireMockConfiguration config) {
        this.config = config;
        return (T) this;
    }

    /**
     * A builder method that enables verbose logging for the tester. Ignored if {@link #config(WireMockConfiguration)}
     * was called explicitly.
     */
    public T verbose() {
        this.verbose = true;
        return (T) this;
    }

    @Override
    public void beforeScope(BQTestScope scope, ExtensionContext context) {
        ensureRunning();
    }

    @Override
    public void afterScope(BQTestScope scope, ExtensionContext context) {
        if (wiremockServer != null) {
            wiremockServer.shutdown();
        }
    }

    /**
     * Returns a Bootique module that can be used to configure a named Jersey client "target" in test {@link io.bootique.BQRuntime}.
     * This method can be used to initialize one or more BQRuntimes in a test class, so that they can share the Wiremock instance.
     *
     * @param targetName the name of the mapped target in {@link io.bootique.jersey.client.HttpTargets}.
     */
    public BQModule moduleWithTestTarget(String targetName) {
        String propName = "bq.jerseyclient.targets." + targetName + ".url";
        return b -> BQCoreModule.extend(b).setProperty(propName, getUrl());
    }

    public Integer getPort() {
        return ensureRunning().port();
    }

    /**
     * Returns the URL of the internal WireMock server.
     */
    public abstract String getUrl();

    protected WireMockServer ensureRunning() {
        if (wiremockServer == null) {
            synchronized (this) {
                if (wiremockServer == null) {
                    this.wiremockServer = createServer();
                    startServer();
                }
            }
        }

        return wiremockServer;
    }

    protected WireMockConfiguration ensureServerConfig() {
        return this.config != null ? this.config : createServerConfig();
    }

    protected WireMockConfiguration createServerConfig() {
        return WireMockConfiguration
                .wireMockConfig()
                .dynamicPort()
                .notifier(new Slf4jNotifier(verbose));
    }

    protected WireMockServer createServer() {
        WireMockConfiguration config = ensureServerConfig();
        return new WireMockServer(config);
    }

    protected void startServer() {
        wiremockServer.start();
        LOGGER.info("WireMock started on port {}", wiremockServer.port());
    }
}
