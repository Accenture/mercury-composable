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

package org.platformlambda.mini.kafka;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.EventEnvelope;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The probe client must be constructible on a thread whose context classloader cannot see
 * {@code kafka-clients}.
 *
 * <p><b>The field failure this pins.</b> A deployment logged, every 5 seconds and 266 times over,
 * {@code Invalid value org.apache.kafka.common.serialization.StringDeserializer for configuration
 * key.deserializer: Class ... could not be found} - while the same JVM's real producers and consumers,
 * using the same jar, worked fine. Not a Kafka problem and not a packaging problem: a classloading one.
 * Kafka resolves a deserializer given as a class NAME through
 * {@code Utils.getContextOrKafkaClassLoader()}, which prefers the thread context classloader and falls
 * back to Kafka's own loader only when the TCCL is {@code null}. So a non-null but wrong TCCL breaks the
 * lookup - ironically a null one would have worked. {@code KafkaHealthCheck} is
 * {@code @KernelThreadRunner} and warms up on the platform's kernel-thread executor, and a pooled kernel
 * thread need not have inherited the application's loader; {@code KafkaFlowAdapter}, which builds a
 * consumer from the identical class-name config and is <em>not</em> a kernel-thread runner, was
 * unaffected. That difference is the whole diagnosis.</p>
 *
 * <p>The fix is to hand the constructor deserializer <b>instances</b>: Kafka then stores
 * {@code keyDeserializer.getClass()} - a {@code Class} object, not a name - and
 * {@code ConfigDef.parseType} returns it directly instead of asking any classloader. The probe already
 * hardcoded exactly these two deserializers, so it is semantically identical and strictly more robust.
 * The alternative, setting the TCCL around construction, mutates global thread state and was rejected.</p>
 *
 * <p>This reproduces the production condition with no broker and no container: a parentless, empty
 * {@link URLClassLoader} as the thread's context loader is a loader that genuinely cannot see
 * {@code kafka-clients}. Before the fix this test fails - {@code buildClient} swallows the
 * {@code ConfigException} (it arrives as a {@code KafkaException}) and returns {@code null}, which the
 * caller reports as a <em>passing</em> "Waiting for Kafka connection" status. That passing-while-broken
 * semantics is what let this hide in the field.</p>
 */
class KafkaHealthCheckClassLoaderTest {

    private static final long PROBE_IMMEDIATELY = 0L;
    private static final long TIMEOUT_MS = 1L;

    /**
     * Config with the deserializers given as class NAMES - deliberately the HARDER case. KafkaClientConfig
     * puts {@code Class} objects, which need no loader at all; names are what exposes a wrong one, so the
     * fix is proven against them rather than against the easier shape production actually ships.
     */
    private static Properties probeConfig() {
        Properties p = new Properties();
        p.setProperty("bootstrap.servers", "localhost:1");
        p.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        p.setProperty("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        return p;
    }

    private static KafkaHealthCheck probing(Supplier<Properties> config) {
        return new KafkaHealthCheck("kafka", config, TIMEOUT_MS, PROBE_IMMEDIATELY);
    }

    /**
     * Run on a thread whose context classloader is parentless and empty - it can load nothing at all,
     * which is the strongest form of "cannot see kafka-clients".
     */
    private static <T> T onThreadWithBlindContextClassLoader(java.util.function.Supplier<T> work)
            throws InterruptedException {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread t = new Thread(() -> {
            try (URLClassLoader blind = new URLClassLoader(new URL[0], null)) {
                Thread.currentThread().setContextClassLoader(blind);
                result.set(work.get());
            } catch (Throwable e) {
                failure.set(e);
            }
        }, "blind-tccl");
        t.start();
        t.join(30_000);
        if (failure.get() != null) {
            throw new AssertionError("the probe threw instead of building or returning null", failure.get());
        }
        return result.get();
    }

    @Test
    void theProbeClientBuildsEvenWhenTheContextClassLoaderCannotSeeKafka() throws InterruptedException {
        var health = probing(KafkaHealthCheckClassLoaderTest::probeConfig);
        try (KafkaConsumer<String, byte[]> consumer =
                     onThreadWithBlindContextClassLoader(() -> health.buildClient(probeConfig()))) {
            assertNotNull(consumer,
                    "a pooled kernel thread's context classloader must not decide whether the probe can "
                            + "be built - buildClient overrides the loader for the construction");
        }
    }

    @Test
    void theSameConfigStillBuildsOnAnOrdinaryThread() {
        // guards against "fixing" the blind-loader case by breaking the normal one
        var health = probing(KafkaHealthCheckClassLoaderTest::probeConfig);
        try (KafkaConsumer<String, byte[]> consumer = health.buildClient(probeConfig())) {
            assertNotNull(consumer, "the ordinary path must be unaffected");
        }
    }

    @Test
    void theContextClassLoaderIsRestoredAfterwards() throws InterruptedException {
        // The probe runs on a POOLED kernel thread, so an override that leaked would follow the thread
        // into whatever it does next. Scoped-and-restored is the whole reason this is acceptable at all.
        var health = probing(KafkaHealthCheckClassLoaderTest::probeConfig);
        AtomicReference<ClassLoader> during = new AtomicReference<>();
        AtomicReference<ClassLoader> after = new AtomicReference<>();
        Thread t = new Thread(() -> {
            try (URLClassLoader marker = new URLClassLoader(new URL[0], null)) {
                Thread.currentThread().setContextClassLoader(marker);
                during.set(marker);
                KafkaConsumer<String, byte[]> consumer = health.buildClient(probeConfig());
                if (consumer != null) {
                    consumer.close();
                }
                after.set(Thread.currentThread().getContextClassLoader());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }, "restore-check");
        t.start();
        t.join(30_000);
        assertSame(during.get(), after.get(),
                "buildClient must restore the thread's original context classloader, even though it "
                        + "overrode it to construct the client");
    }

    @Test
    void aGenuinelyUnusableTemplateStillReturnsNull() {
        // the waiting-status contract must survive the fix: an empty template is still unbuildable
        var health = probing(Properties::new);
        assertNull(health.buildClient(new Properties()),
                "an incomplete template is still a start-up condition, reported as passing/waiting");
    }

    @Test
    void aClassThatIsNotOnTheClasspathFailsHealthInsteadOfWaitingForever() {
        // The leniency exists for ONE case: a value a later credential bootstrap will publish. A class
        // that simply is not there will never arrive by waiting, and reporting it as passing is how the
        // field's probe stayed "healthy" while logging an unresolvable class every 5 seconds for hours.
        Properties bad = probeConfig();
        bad.setProperty("partition.assignment.strategy", "com.example.NoSuchAssignor");
        var health = probing(() -> bad);
        assertThrows(KafkaHealthCheck.UnusableConfigException.class, () -> health.buildClient(bad),
                "a class that cannot be found is a deployment defect, not a start-up condition");
    }

    @Test
    void anUnusableConfigSurfacesAs503AndNamesTheConfigurationNotTheNetwork() {
        Properties bad = probeConfig();
        bad.setProperty("partition.assignment.strategy", "com.example.NoSuchAssignor");
        var health = probing(() -> bad);
        Object answer = health.handleEvent(Map.of("type", "health"), null, 1);
        assertInstanceOf(EventEnvelope.class, answer, "an unusable configuration must fail /health");
        EventEnvelope envelope = (EventEnvelope) answer;
        assertEquals(503, envelope.getStatus());
        assertInstanceOf(Map.class, envelope.getBody());
        String text = String.valueOf(((Map<?, ?>) envelope.getBody()).get("text"));
        assertTrue(text.contains("configuration is unusable"),
                "point the reader at the classpath, not the network: " + text);
        assertFalse(text.contains("cluster is not reachable"),
                "this is not an outage - saying so would send the reader the wrong way: " + text);
    }
}
