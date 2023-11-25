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

package io.bootique.jersey.client.instrumented;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import io.bootique.metrics.MetricNaming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import java.util.List;
import java.util.Map;

/**
 * @deprecated The users are encouraged to switch to the Jakarta-based flavor
 */
@Deprecated(since = "3.0", forRemoval = true)
// The priority must be higher than that of RequestLogger, so that the Timer could print its output inside the Logger
@Priority(200)
public class RequestTimer implements ClientRequestFilter, ClientResponseFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestTimer.class);
    private static final String TIMER_PROPERTY = RequestTimer.class.getName() + ".timer";
    public static final String TIMER_NAME = MetricNaming.forModule(JerseyClientInstrumentedModule.class).name("Client", "RequestTimer");

    private final Timer requestTimer;

    public RequestTimer(MetricRegistry metricRegistry) {
        this.requestTimer = metricRegistry.timer(TIMER_NAME);
    }

    @Override
    public void filter(ClientRequestContext requestContext) {
        Timer.Context requestTimerContext = requestTimer.time();
        requestContext.setProperty(TIMER_PROPERTY, requestTimerContext);

        LOGGER.info("started");

        // note that response filter method may not be called at all if the
        // request results in connection exception, etc... Would be nice to
        // trace failed requests too, but nothing in JAX RS allows us to do
        // that directly...
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {

        if (LOGGER.isDebugEnabled()) {
            for (Map.Entry<String, List<String>> e : responseContext.getHeaders().entrySet()) {
                for (String header : e.getValue()) {
                    LOGGER.debug("< " + e.getKey() + ": " + header);
                }
            }
        }

        Timer.Context requestTimerContext = (Timer.Context) requestContext.getProperty(TIMER_PROPERTY);

        // TODO: this timing does not take into account reading response content...
        //  May need to add additional interceptor for that.
        long timeNanos = requestTimerContext.stop();
        LOGGER.info("finished in {} ms", timeNanos / 1000000);
    }
}
