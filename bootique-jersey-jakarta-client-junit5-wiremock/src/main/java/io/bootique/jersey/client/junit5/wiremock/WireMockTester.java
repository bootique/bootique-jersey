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
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import io.bootique.BQCoreModule;
import io.bootique.di.BQModule;
import io.bootique.junit5.BQTestScope;
import io.bootique.junit5.scope.BQAfterScopeCallback;
import io.bootique.junit5.scope.BQBeforeScopeCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * A Bootique test tool that sets up and manages a WireMock "server". Each tester should be annotated with
 * {@link io.bootique.junit5.BQTestTool}. The tester supports manually configured request "stubs" as well as proxying
 * a real backend and caching that backend's responses as local "snapshot" files.
 *
 * @since 3.0
 */
public class WireMockTester implements BQBeforeScopeCallback, BQAfterScopeCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(WireMockTester.class);

    private final List<StubMapping> stubs;
    private boolean verbose;
    private WireMockTesterProxy proxy;
    private String filesRoot;
    private UnaryOperator<WireMockConfiguration> configCustomizer;

    protected volatile WireMockServer server;

    public static WireMockTester create() {
        return new WireMockTester();
    }

    protected WireMockTester() {
        this.stubs = new ArrayList<>();
    }

    public WireMockTester stub(MappingBuilder mappingBuilder) {
        this.stubs.add(mappingBuilder.build());
        return this;
    }

    /**
     * A builder method that adds a special stub with minimal priority that will proxy all requests to the specified
     * real backend service (aka "origin"). If "takeLocalSnapshots" is true, each proxy call will result in creation
     * of a local snapshot file with the origin response. In this case all subsequent calls to this URL will result
     * in responses generated locally without going to the proxied origin.
     */
    public WireMockTester proxyTo(String proxyToUrl, boolean takeLocalSnapshots) {
        this.proxy = new WireMockTesterProxy(proxyToUrl, takeLocalSnapshots);
        return this;
    }

    /**
     * A builder method that establishes a local directory that will be used as a root for WireMock recording files.
     * If not set, a default location will be picked automatically.
     */
    public WireMockTester filesRoot(String path) {
        this.filesRoot = path;
        return this;
    }

    /**
     * A builder method that establishes a local directory that will be used as a root for WireMock recording files.
     * If not set, a default location will be picked automatically.
     */
    public WireMockTester filesRoot(File dir) {
        return filesRoot(dir.getAbsolutePath());
    }

    /**
     * A builder method to set a function that customizes tester-provided configuration
     */
    public WireMockTester configCustomizer(UnaryOperator<WireMockConfiguration> configCustomizer) {
        this.configCustomizer = configCustomizer;
        return this;
    }

    /**
     * A builder method that enables verbose logging for the tester.
     */
    public WireMockTester verbose() {
        this.verbose = true;
        return this;
    }

    @Override
    public void beforeScope(BQTestScope scope, ExtensionContext context) {
        ensureRunning();
    }

    @Override
    public void afterScope(BQTestScope scope, ExtensionContext context) {
        if (server != null) {
            server.shutdown();
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
    public String getUrl() {
        return ensureRunning().baseUrl();
    }

    protected WireMockServer ensureRunning() {
        if (server == null) {
            synchronized (this) {
                if (server == null) {

                    WireMockConfiguration config = createServerConfig();
                    WireMockServer server = createServer(config);
                    startServer(server);

                    this.server = server;
                }
            }
        }

        return server;
    }

    protected WireMockConfiguration createServerConfig() {

        WireMockConfiguration config = WireMockConfiguration
                .wireMockConfig()
                .dynamicPort()
                .notifier(new Slf4jNotifier(verbose));

        if (filesRoot != null) {
            config.usingFilesUnderDirectory(filesRoot);
        }

        if (proxy != null) {
            // this will result in snapshot recording after every request
            config.extensions(proxy.createSnapshotRecorder());
        }

        return configCustomizer != null ? configCustomizer.apply(config) : config;
    }

    protected WireMockServer createServer(WireMockConfiguration config) {
        WireMockServer server = new WireMockServer(config);
        installStubs(server);
        return server;
    }

    protected void installStubs(WireMockServer server) {
        stubs.forEach(server::addStubMapping);

        if (proxy != null) {
            server.addStubMapping(proxy.createStub(stubs));
        }
    }

    protected void startServer(WireMockServer server) {
        server.start();
        LOGGER.info("WireMock started on port {}", server.port());
    }
}
