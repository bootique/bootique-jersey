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
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.core.WireMockApp;
import com.github.tomakehurst.wiremock.recording.RecordSpecBuilder;
import com.github.tomakehurst.wiremock.recording.SnapshotRecordResult;
import com.github.tomakehurst.wiremock.standalone.JsonFileMappingsSource;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

/**
 * A custom Wiremock recorder that keeps "scenarios" in separate directories per test.
 *
 * @since 3.0
 */
public class WireMockRecorder {

    public static void startRecording(WireMockServer wiremock, String targetUrl) {
        wiremock.startRecording(new RecordSpecBuilder()
                .forTarget(targetUrl)
                .extractTextBodiesOver(9_999_999)
                .extractBinaryBodiesOver(9_999_999)
                .ignoreRepeatRequests()
                .makeStubsPersistent(false) // we have our own persistence implementation
                .build()
        );
    }

    public static void stopRecording(WireMockServer wiremock, ExtensionContext context) throws IOException {
        SnapshotRecordResult recordingResult = wiremock.stopRecording();
        String rootDir = wiremock.getOptions().filesRoot().getPath();
        saveScenarios(recordingResult.getStubMappings(), rootDir, context);
    }

    private static void saveScenarios(List<StubMapping> scenarios, String rootDir, ExtensionContext context) throws IOException {
        if (!scenarios.isEmpty()) {
            new JsonFileMappingsSource(prepareRecordingDir(rootDir, context)).save(scenarios);
        }
    }

    private static FileSource prepareRecordingDir(String rootDir, ExtensionContext context) throws IOException {
        String testClass = context.getRequiredTestClass().getName();
        String testMethod = context.getRequiredTestMethod().getName();
        Path recordingsPath = Paths.get(rootDir, WireMockApp.MAPPINGS_ROOT, testClass, testMethod);

        // create path if it does not exist
        if (!Files.exists(recordingsPath)) {
            Files.createDirectories(recordingsPath);
        }

        // cleanup old files
        try (Stream<Path> stream = Files.list(recordingsPath)) {
            stream.map(Path::toFile).forEach(File::delete);
        }

        return new SingleRootFileSource(recordingsPath.toString());
    }
}
