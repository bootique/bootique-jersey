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
package io.bootique.jersey.client.instrumented.mdc;

import io.bootique.metrics.mdc.TransactionIdMDC;

import java.util.concurrent.*;

/**
 * @since 3.0
 * @deprecated The users are encouraged to switch to the Jakarta-based flavor
 */
@Deprecated(since = "3.0", forRemoval = true)
public class MDCAwareThreadPoolExecutor extends ThreadPoolExecutor {

    public MDCAwareThreadPoolExecutor(
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            ThreadFactory threadFactory,
            RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        // propagate tx id to the execution threads if it exists on the calling thread
        String txId = TransactionIdMDC.getId();
        return txId != null
                ? new TxFutureTask<>(callable, txId)
                : new FutureTask<>(callable);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        // propagate tx id to the execution threads if it exists on the calling thread
        String txId = TransactionIdMDC.getId();
        return txId != null
                ? new TxFutureTask<>(runnable, value, txId)
                : new FutureTask<>(runnable, value);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        if (r instanceof TxFutureTask) {
            TransactionIdMDC.setId(((TxFutureTask<?>) r).txId);
        }
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        if (r instanceof TxFutureTask) {
            TransactionIdMDC.clearId();
        }
    }

    static class TxFutureTask<V> extends FutureTask<V> {
        final String txId;

        TxFutureTask(Callable<V> callable, String txId) {
            super(callable);
            this.txId = txId;
        }

        TxFutureTask(Runnable runnable, V result, String txId) {
            super(runnable, result);
            this.txId = txId;
        }
    }
}
