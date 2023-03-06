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
import io.bootique.junit5.scope.BQAfterMethodCallback;
import io.bootique.junit5.scope.BQAfterScopeCallback;
import io.bootique.junit5.scope.BQBeforeMethodCallback;
import io.bootique.junit5.scope.BQBeforeScopeCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * A Bootique test tool that customises and manages Wiremock lifecycle: server start/stop, recording start/stop.
 * Should be annotated with {@link io.bootique.junit5.BQTestTool}. To enable the recording mode run tests with
 * {code}-Dbq.wiremock.recording{code} argument. Wiremock will proxy targetUrl and persist responses as mappings.
 *
 * @since 3.0
 */
public class WireMockTester implements BQBeforeScopeCallback, BQAfterScopeCallback, BQBeforeMethodCallback, BQAfterMethodCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(WireMockTester.class);
    private static final File DEFAULT_FILES_ROOT = new File("src/test/resources/wiremock/");

    /**
     * If present, enables wiremock recording mode for all tests
     */
    public static final String RECORDING_PROPERTY = "bq.wiremock.recording";
    private static final boolean IS_RECORDING_ENABLED = System.getProperty(RECORDING_PROPERTY) != null;

    private final String targetUrl;
    private final String folder;
    private WireMockConfiguration config;
    private File filesRoot;

    private volatile WireMockServer wiremockServer;

    /**
     * @param targetUrl url of resource to mock, for example: "https://www.example.com/foo/bar"
     */
    public static WireMockTester tester(String targetUrl) {
        return new WireMockTester(targetUrl);
    }

    protected WireMockTester(String targetUrl) {
        this.targetUrl = Objects.requireNonNull(targetUrl);
        this.folder = convertToFolderName(targetUrl);
    }

    /**
     * A builder method that sets an optional custom config for WireMockServer. Overrides all customization made to the
     * tester.
     */
    public WireMockTester config(WireMockConfiguration config) {
        this.config = config;

        // reset all custom configs
        this.filesRoot = null;
        return this;
    }

    /**
     * A builder method that establishes a local directory that will be used as a root for WireMock recording files.
     * If not set, a default location will be picked automatically.
     */
    public WireMockTester filesRoot(File filesRoot) {
        this.filesRoot = filesRoot;

        // reset custom config override
        this.config = null;
        return this;
    }

    /**
     * A builder method that establishes a local directory that will be used as a root for WireMock recording files.
     * If not set, a default location will be picked automatically.
     */
    public WireMockTester filesRoot(String filesRoot) {
        return filesRoot(new File(filesRoot));
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
        return b -> BQCoreModule.extend(b).setProperty(propName, url());
    }

    public Integer port() {
        return ensureRunning().port();
    }

    public String url() {
        return ensureRunning().url("");
    }

    /**
     * The name of the tester-specific folder located in the wiremock scenarios base folder.
     */
    public String folder() {
        return folder;
    }

    private WireMockServer ensureRunning() {
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

    private WireMockServer createServer() {
        WireMockConfiguration config = createServerConfig();
        return new WireMockServer(config);
    }

    private WireMockConfiguration createServerConfig() {
        if (this.config != null) {
            return this.config;
        }

        // TODO: the default must be portable and should not assume Maven project structure
        File filesRoot = this.filesRoot != null ? this.filesRoot : DEFAULT_FILES_ROOT;
        File testerRoot = new File(filesRoot, folder);

        return WireMockConfiguration
                .wireMockConfig()
                .dynamicPort()
                .notifier(new Slf4jNotifier(true))
                .usingFilesUnderDirectory(testerRoot.getAbsolutePath());
    }

    private void startServer() {
        wiremockServer.start();

        if (IS_RECORDING_ENABLED) {
            LOGGER.info("Wiremock started in recording mode on port {}", wiremockServer.port());
        } else {
            LOGGER.info("Wiremock started on port {}", wiremockServer.port());
        }
    }

    @Override
    public void beforeMethod(BQTestScope scope, ExtensionContext context) {
        if (IS_RECORDING_ENABLED) {
            WireMockRecorder.startRecording(wiremockServer, targetUrl);
        }
    }

    @Override
    public void afterMethod(BQTestScope scope, ExtensionContext context) throws IOException {
        if (IS_RECORDING_ENABLED) {
            WireMockRecorder.stopRecording(wiremockServer, context);
        }
    }

    static String convertToFolderName(String url) {
        int len = url.length();
        StringBuilder out = new StringBuilder(len);

        boolean wasReplaced = false;
        for (int i = 0; i < len; i++) {
            char c = url.charAt(i);
            switch (c) {
                case '.':
                case ':':
                case '/':
                case '?':
                case '&':

                    // squash repeating _'s
                    if (!wasReplaced) {
                        out.append('_');
                        wasReplaced = true;
                    }

                    break;

                default:
                    wasReplaced = false;
                    out.append(c);
                    break;
            }
        }

        return out.toString();
    }
}
