/*

    Copyright 2018-2026 Accenture Technology

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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.util.ElasticQueue;
import org.platformlambda.core.util.Utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceQueueTest {

    private static final String STORE_PROP = "elastic.queue.store";
    private static final String MAILBOX_PROP = "elastic.queue.dispatch.mailbox.size";

    @AfterEach
    void clearOverrides() {
        System.clearProperty(STORE_PROP);
        System.clearProperty(MAILBOX_PROP);
    }

    @Test
    void fileDispatchMailboxUsesBoundedConfiguredCapacity() throws InterruptedException {
        System.setProperty(STORE_PROP, "file");
        System.setProperty(MAILBOX_PROP, "64");
        String route = "service.queue.mailbox." + Utility.getInstance().getUuid();
        TypedLambdaFunction<Object, Object> fn = (headers, input, instance) -> true;
        ServiceQueue queue = new ServiceQueue(new ServiceDef(route, fn));
        try {
            Utility.getInstance().sleep(100);
            assertEquals(64, ServiceQueue.dispatchMailboxSize());
            assertEquals(64, queue.getDispatchMailboxRemainingCapacity());
        } finally {
            queue.stop();
        }
    }

    @Test
    void dispatchMailboxSizeIsClampedToElasticMemoryBuffer() {
        System.setProperty(MAILBOX_PROP, "1");
        assertEquals(ElasticQueue.MEMORY_BUFFER, ServiceQueue.dispatchMailboxSize());
        System.setProperty(MAILBOX_PROP, "-1");
        assertTrue(ServiceQueue.dispatchMailboxSize() >= ElasticQueue.MEMORY_BUFFER);
    }
}
