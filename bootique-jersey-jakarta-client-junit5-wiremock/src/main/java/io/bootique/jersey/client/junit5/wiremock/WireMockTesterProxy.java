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
import com.github.tomakehurst.wiremock.recording.RecordSpec;
import com.github.tomakehurst.wiremock.recording.RecordSpecBuilder;
import com.github.tomakehurst.wiremock.recording.SnapshotRecordResult;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.proxyAllTo;

class WireMockTesterProxy {

    private final String proxyToUrl;
    private final boolean takeLocalSnapshots;
    private final RecordSpec snapshotSpec;

    public WireMockTesterProxy(String proxyToUrl, boolean takeLocalSnapshots) {
        this.proxyToUrl = Objects.requireNonNull(proxyToUrl);
        this.takeLocalSnapshots = takeLocalSnapshots;
        this.snapshotSpec = new RecordSpecBuilder()
                .forTarget(proxyToUrl)
                .extractTextBodiesOver(9_999_999)
                .extractBinaryBodiesOver(9_999_999)
                .ignoreRepeatRequests()

                // do not persist via WireMock as we have our own stub persistence implementation
                .makeStubsPersistent(false)
                .build();
    }

    StubMapping createStub(List<StubMapping> afterStubs) {

        // proxy stub is a "catch all" stub. So its priority value must be higher (i.e. lower priority)
        // than all the explicit stubs

        int maxPriority = StubMapping.DEFAULT_PRIORITY;
        for (StubMapping s : afterStubs) {
            if (s.getPriority() != null) {
                maxPriority = Math.max(maxPriority, s.getPriority());
            }
        }

        return proxyAllTo(proxyToUrl).atPriority(maxPriority + 1).build();
    }

    void saveSnapshotIfNeeded(WireMockServer wireMockServer, ExtensionContext context) throws IOException {
        if (takeLocalSnapshots) {
            SnapshotRecordResult snapshot = wireMockServer.snapshotRecord(snapshotSpec);
            WireMockSnapshotSaver.save(wireMockServer, snapshot, context);
        }
    }
}
