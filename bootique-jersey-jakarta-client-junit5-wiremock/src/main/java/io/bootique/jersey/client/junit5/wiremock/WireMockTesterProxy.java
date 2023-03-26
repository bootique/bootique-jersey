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

import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.core.WireMockApp;
import com.github.tomakehurst.wiremock.extension.PostServeAction;
import com.github.tomakehurst.wiremock.recording.RecordSpec;
import com.github.tomakehurst.wiremock.recording.RecordSpecBuilder;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import java.util.List;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.proxyAllTo;

class WireMockTesterProxy {

    private final String originUrl;
    private final boolean takeLocalSnapshots;
    private final RecordSpec snapshotSpec;

    public WireMockTesterProxy(String originUrl, boolean takeLocalSnapshots) {
        this.originUrl = Objects.requireNonNull(originUrl);
        this.takeLocalSnapshots = takeLocalSnapshots;
        this.snapshotSpec = new RecordSpecBuilder()
                .forTarget(originUrl)
                .extractTextBodiesOver(9_999_999)
                .extractBinaryBodiesOver(9_999_999)
                .ignoreRepeatRequests()
                .makeStubsPersistent(true)
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

        return proxyAllTo(originUrl).atPriority(maxPriority + 1).build();
    }

    PostServeAction createSnapshotRecorder() {
        return new PostServeAction() {
            @Override
            public String getName() {
                return "bq-snapshot-recorder";
            }

            @Override
            public void doGlobalAction(ServeEvent serveEvent, Admin admin) {
                saveSnapshotIfNeeded(admin);
            }
        };
    }

    void saveSnapshotIfNeeded(Admin admin) {
        if (takeLocalSnapshots) {
            admin.getOptions().filesRoot().child(WireMockApp.MAPPINGS_ROOT).createIfNecessary();
            admin.snapshotRecord(snapshotSpec);

            // this will allow
            admin.resetRequests();
        }
    }
}
