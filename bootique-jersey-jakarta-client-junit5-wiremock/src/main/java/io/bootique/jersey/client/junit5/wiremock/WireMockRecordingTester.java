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

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.recording.RecordSpecBuilder;
import com.github.tomakehurst.wiremock.recording.SnapshotRecordResult;
import io.bootique.junit5.BQTestScope;
import io.bootique.junit5.scope.BQAfterMethodCallback;
import io.bootique.junit5.scope.BQBeforeMethodCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * A WireMockTester that creates stubs by recording the requests from an actual external service. To enable the
 * recording mode for all testers, start tests with {code}-Dbq.wiremock.recording{code} argument, or call
 * {@link #recordingEnabled()} on individual testers. If enabled, WireMock will proxy the target URL and persist
 * responses as mappings.
 *
 * @since 3.0
 */
public class WireMockRecordingTester extends WireMockTester<WireMockRecordingTester> implements BQBeforeMethodCallback, BQAfterMethodCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(WireMockRecordingTester.class);

    // TODO: the default must be portable and should not assume Maven project structure
    private static final File DEFAULT_FILES_ROOT = new File("src/test/resources/wiremock/");

    /**
     * If present, enables wiremock recording mode for all tests
     */
    public static final String RECORDING_PROPERTY = "bq.wiremock.recording";

    private final WireMockUrlParts urlParts;
    private File filesRoot;
    private boolean recordingEnabled;

    protected WireMockRecordingTester(WireMockUrlParts urlParts) {
        this.urlParts = Objects.requireNonNull(urlParts);
        this.recordingEnabled = System.getProperty(RECORDING_PROPERTY) != null;
    }

    /**
     * Enables the recording mode for this tester. It will result in this tester going to the origin for the request
     * data and storing it in a file. The same can be achieved for all testers at once by starting the tests JVM with
     * {code}-Dbq.wiremock.recording{code} property.
     */
    public WireMockRecordingTester recordingEnabled() {
        this.recordingEnabled = true;
        return this;
    }

    /**
     * A builder method that establishes a local directory that will be used as a root for WireMock recording files.
     * If not set, a default location will be picked automatically.
     */
    public WireMockRecordingTester filesRoot(File filesRoot) {
        this.filesRoot = filesRoot;

        // reset custom config override
        this.config = null;
        return this;
    }

    /**
     * A builder method that establishes a local directory that will be used as a root for WireMock recording files.
     * If not set, a default location will be picked automatically.
     */
    public WireMockRecordingTester filesRoot(String filesRoot) {
        return filesRoot(new File(filesRoot));
    }

    /**
     * Returns the target URL of the tester rewritten to access the WireMock proxy.
     */
    @Override
    public String getUrl() {
        return ensureRunning().baseUrl() + urlParts.getPath();
    }

    @Override
    protected WireMockConfiguration createServerConfig() {
        File filesRoot = this.filesRoot != null ? this.filesRoot : DEFAULT_FILES_ROOT;
        File testerRoot = new File(filesRoot, urlParts.getTesterFolder());

        return super.createServerConfig()
                .usingFilesUnderDirectory(testerRoot.getAbsolutePath());
    }

    @Override
    protected void startServer() {
        super.startServer();
        if (recordingEnabled) {
            LOGGER.info("WireMock started in recording mode");
        }
    }

    @Override
    public void beforeMethod(BQTestScope scope, ExtensionContext context) {
        if (recordingEnabled) {

            // note that we are recording relative to the "baseUrl", not the full target URL
            wireMockServer.startRecording(new RecordSpecBuilder()
                    .forTarget(urlParts.getBaseUrl())
                    .extractTextBodiesOver(9_999_999)
                    .extractBinaryBodiesOver(9_999_999)
                    .ignoreRepeatRequests()
                    .makeStubsPersistent(false) // we have our own stub persistence implementation
                    .build()
            );
        }
    }

    @Override
    public void afterMethod(BQTestScope scope, ExtensionContext context) throws IOException {
        if (recordingEnabled) {
            SnapshotRecordResult snapshot = wireMockServer.stopRecording();
            WireMockSnapshotSaver.save(wireMockServer, snapshot, context);
        }
    }
}
