package org.platformlambda.core;

import org.junit.jupiter.api.Test;
import org.platformlambda.common.TestBase;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.LocalPubSub;
import org.platformlambda.core.system.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LocalPubSubTest extends TestBase {
    private static final Logger log = LoggerFactory.getLogger(LocalPubSubTest.class);
    private static final String MY_ROUTE = "my_route";
    private static final String TEST_TOPIC_ONE = "test.topic.one";
    private static final String SUBSCRIBER_ONE = "subscriber.one";
    private static final String SUBSCRIBER_TWO = "subscriber.two";

    @Test
    void subscriptionTest() throws IOException, InterruptedException {
        LocalPubSub ps = LocalPubSub.getInstance();
        ps.createTopic(TEST_TOPIC_ONE);
        // you can subscribe before you create the subscriber services
        boolean subscribe1 = ps.subscribe(TEST_TOPIC_ONE, SUBSCRIBER_ONE);
        boolean subscribe2 =ps.subscribe(TEST_TOPIC_ONE, SUBSCRIBER_TWO);
        assertTrue(ps.topicExists(TEST_TOPIC_ONE));
        assertTrue(subscribe1);
        assertTrue(subscribe2);
        List<String> members = ps.getSubscribers(TEST_TOPIC_ONE);
        assertTrue(members.contains(SUBSCRIBER_ONE));
        assertTrue(members.contains(SUBSCRIBER_TWO));
        List<String> topics = ps.getTopics();
        assertTrue(topics.contains(TEST_TOPIC_ONE));
        // now register the subscribers and send an event to the topic
        final AtomicInteger counter = new AtomicInteger(0);
        final BlockingQueue<Boolean> completion = new ArrayBlockingQueue<>(1);
        final ConcurrentMap<String, Object> result = new ConcurrentHashMap<>();
        LambdaFunction f = (headers, input, instance) -> {
            String myRoute = headers.get(MY_ROUTE);
            result.put(myRoute, input);
            if (counter.incrementAndGet() == 2) {
                completion.add(true);
            }
            return true;
        };
        Platform platform = Platform.getInstance();
        platform.register(SUBSCRIBER_ONE, f, 1);
        platform.registerPrivate(SUBSCRIBER_TWO, f, 1);
        final EventEmitter po = EventEmitter.getInstance();
        final String TEXT = "test message";
        po.send(TEST_TOPIC_ONE, TEXT);
        completion.poll(5, TimeUnit.SECONDS);
        assertEquals(2, result.size());
        for (Map.Entry<String, Object> kv: result.entrySet()) {
            assertEquals(TEXT, kv.getValue());
            log.info("Result from {} is correct", kv.getKey());
        }
        ps.unsubscribe(TEST_TOPIC_ONE, SUBSCRIBER_ONE);
        List<String> membersAgain = ps.getSubscribers(TEST_TOPIC_ONE);
        assertFalse(membersAgain.contains(SUBSCRIBER_ONE));
        assertTrue(membersAgain.contains(SUBSCRIBER_TWO));
        // When you delete a topic, its subscriptions, if any, will be void.
        ps.deleteTopic(TEST_TOPIC_ONE);
        List<String> topicsAgain = ps.getTopics();
        assertFalse(topicsAgain.contains(TEST_TOPIC_ONE));
    }
}
