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
import com.github.tomakehurst.wiremock.recording.SnapshotRecordResult;
import com.github.tomakehurst.wiremock.standalone.JsonFileMappingsSource;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A custom Wiremock recorder that keeps recorded scenarios in separate directories per test.
 *
 * @since 3.0
 */
class WireMockSnapshotSaver {

    public static void save(WireMockServer wireMockServer, SnapshotRecordResult snapshot, ExtensionContext context) throws IOException {
        String rootDir = wireMockServer.getOptions().filesRoot().getPath();
        if (!snapshot.getStubMappings().isEmpty()) {
            FileSource fileSource = prepareRecordingDir(rootDir, context);
            new JsonFileMappingsSource(fileSource).save(snapshot.getStubMappings());
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
        
        return new SingleRootFileSource(recordingsPath.toString());
    }
}
