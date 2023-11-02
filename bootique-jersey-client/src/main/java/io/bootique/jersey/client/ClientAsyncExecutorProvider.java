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
package io.bootique.jersey.client;

import org.glassfish.jersey.client.ClientAsyncExecutor;
import org.glassfish.jersey.client.internal.LocalizationMessages;
import org.glassfish.jersey.internal.util.collection.LazyValue;
import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.internal.util.collection.Values;
import org.glassfish.jersey.spi.ThreadPoolExecutorProvider;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.logging.Logger;

// a copy of non-public Jersey DefaultClientAsyncExecutorProvider that allows us to customize async pool parameters
@ClientAsyncExecutor
public class ClientAsyncExecutorProvider extends ThreadPoolExecutorProvider {

    private static final Logger LOGGER = Logger.getLogger(ClientAsyncExecutorProvider.class.getName());

    private final LazyValue<Integer> asyncThreadPoolSize;

    @Inject
    public ClientAsyncExecutorProvider(@Named("ClientAsyncThreadPoolSize") final int poolSize) {

        super("bootique-http-client-async");

        this.asyncThreadPoolSize = Values.lazy((Value<Integer>) () -> {
            if (poolSize <= 0) {
                LOGGER.config(LocalizationMessages.IGNORED_ASYNC_THREADPOOL_SIZE(poolSize));
                return Integer.MAX_VALUE;
            } else {
                LOGGER.config(LocalizationMessages.USING_FIXED_ASYNC_THREADPOOL(poolSize));
                return poolSize;
            }
        });
    }

    @Override
    protected int getMaximumPoolSize() {
        return asyncThreadPoolSize.get();
    }

    @Override
    protected int getCorePoolSize() {
        int maximumPoolSize = getMaximumPoolSize();
        return maximumPoolSize < Integer.MAX_VALUE ? maximumPoolSize : 0;
    }
}
