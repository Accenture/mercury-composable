/*

    Copyright 2018-2025 Accenture Technology

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */

package org.platformlambda.core.system;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import org.platformlambda.core.annotations.EventInterceptor;
import org.platformlambda.core.annotations.ZeroTracing;
import org.platformlambda.core.models.EventEnvelope;

public class WorkerDispatcher extends WorkerQueues {
    private static final String MY_ROUTE = "my_route";
    private static final String MY_TRACE_ID = "my_trace_id";
    private static final String MY_TRACE_PATH = "my_trace_path";
    private final boolean tracing;
    private final boolean interceptor;
    private final boolean useEnvelope;
    private final int instance;

    protected WorkerDispatcher(ServiceDef def, String route, int instance) {
        super(def, route);
        this.instance = instance;
        this.tracing = def.getFunction().getClass().getAnnotation(ZeroTracing.class) == null;
        this.interceptor = def.getFunction().getClass().getAnnotation(EventInterceptor.class) != null;
        this.useEnvelope = def.inputIsEnvelope();
        EventBus system = Platform.getInstance().getEventSystem();
        this.consumer = system.localConsumer(route, new DispatchWork());
        // tell manager that this worker is ready to process a new event
        system.send(def.getRoute(), READY+route);
        this.started();
    }

    private class DispatchWork implements Handler<Message<byte[]>> {

        @Override
        public void handle(Message<byte[]> message) {
            if (!stopped) {
                EventEnvelope event = new EventEnvelope(message.body());
                var headers = event.getHeaders();
                headers.remove(MY_ROUTE);
                headers.remove(MY_TRACE_ID);
                headers.remove(MY_TRACE_PATH);
                var executor = def.isVirtualThread()? vThreadExecutor : kernelExecutor;
                executor.submit(()-> {
                    var worker = new WorkerHandler(def, route, instance, tracing, interceptor, useEnvelope);
                    worker.executeFunction(event);
                });
            }
        }
    }
}
