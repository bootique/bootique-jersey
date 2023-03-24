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
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import java.util.ArrayList;
import java.util.List;

/**
 * A WireMockTester that runs a server with manually-created stubs.
 *
 * @since 3.0
 */
public class WireMockStubbingTester extends WireMockTester<WireMockStubbingTester> {

    private List<StubMapping> stubs;

    protected WireMockStubbingTester() {
        this.stubs = new ArrayList<>();
    }

    public WireMockStubbingTester stub(MappingBuilder mappingBuilder) {
        this.stubs.add(mappingBuilder.build());
        return this;
    }

    /**
     * Returns the target URL of the tester rewritten to access the WireMock proxy.
     */

    @Override
    public String getUrl() {
        return ensureRunning().baseUrl();
    }

    @Override
    protected WireMockServer createServer() {
        WireMockServer server = super.createServer();
        stubs.forEach(server::addStubMapping);
        return server;
    }
}
