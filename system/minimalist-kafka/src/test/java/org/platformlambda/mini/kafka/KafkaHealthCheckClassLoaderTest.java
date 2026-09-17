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

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.EventEnvelope;
import org.slf4j.Logger;

import java.lang.reflect.Method;
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
 * <p>The fix is to pin this module's own classloader as the thread context loader for the duration of
 * the work. Handing the constructor deserializer <b>instances</b> was tried first and is not enough:
 * it removes two lookups and the next class-name config fails in their place, because Kafka resolves
 * many configs this way. Fix the loader, not one config at a time.</p>
 *
 * <p><b>Second round (v4.12.12): the same trap, one call earlier.</b> Scoping the loader to client
 * CONSTRUCTION left a produce-only leg still broken in the field, with
 * {@code Class org.apache.kafka.common.security.oauthbearer.DefaultJwtRetriever could not be found}.
 * Kafka consults the loader before any client exists - in the static initializer of its own config
 * classes, which resolve class-valued config DEFAULTS at {@code ConfigDef.define} time. The probe now
 * pins the loader across the whole probe, template resolution included; see
 * {@code KafkaHealthCheck.withModuleClassLoader} for the mechanism and why one bad initialization
 * cannot be undone.</p>
 *
 * <p>This reproduces the production condition with no broker and no container: a parentless, empty
 * {@link URLClassLoader} as the thread's context loader is a loader that genuinely cannot see
 * {@code kafka-clients}. Before the first fix these tests fail - {@code buildClient} swallows the
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

    // ------------------------------------------------------------------------------------------
    // The produce-only leg: 4.12.11 fixed the consumer path and left this one broken
    // ------------------------------------------------------------------------------------------

    /**
     * Resolving the template is itself a Kafka classloading event, so it must happen under the
     * override too - not just the construction that follows it.
     *
     * <p>On a produce-only leg ({@code kafka.consumer.enabled=false}) {@code healthProbeProperties}
     * filters the producer template through {@code ConsumerConfig.configNames()}. That static METHOD
     * call initializes {@code ConsumerConfig}, whose initializer resolves the default value of
     * {@code sasl.oauthbearer.jwt.retriever.class} through the thread context classloader - and on a
     * pooled kernel thread that loader cannot see {@code kafka-clients}. The consumer path is spared
     * only because its references to the same class are compile-time constants that javac inlines.</p>
     *
     * <p>Asserting on the loader the supplier SEES is the durable form of this test: it holds whatever
     * {@code healthProbeProperties} does internally, and it fails on the code as shipped in 4.12.11.</p>
     */
    @Test
    void theTemplateIsResolvedWithTheModuleClassLoaderPinned() throws InterruptedException {
        AtomicReference<ClassLoader> whileResolving = new AtomicReference<>();
        var health = probing(() -> {
            whileResolving.set(Thread.currentThread().getContextClassLoader());
            return probeConfig();
        });
        onThreadWithBlindContextClassLoader(() -> health.handleEvent(Map.of("type", "health"), null, 1));
        assertSame(KafkaHealthCheck.class.getClassLoader(), whileResolving.get(),
                "the probe template must be resolved under the module classloader - a pooled kernel "
                        + "thread's loader decides whether ConsumerConfig can even be initialized");
    }

    /** The same for {@code type=info}, which resolves the template when it arrives before any probe. */
    @Test
    void theInfoPathResolvesWithTheModuleClassLoaderPinned() throws InterruptedException {
        AtomicReference<ClassLoader> whileResolving = new AtomicReference<>();
        var health = probing(() -> {
            whileResolving.set(Thread.currentThread().getContextClassLoader());
            return probeConfig();
        });
        onThreadWithBlindContextClassLoader(() -> health.handleEvent(Map.of("type", "info"), null, 1));
        assertSame(KafkaHealthCheck.class.getClassLoader(), whileResolving.get(),
                "type=info resolves the template too, on the same kernel thread");
    }

    /**
     * A class whose static initializer threw is erroneous for the life of the JVM, so every later
     * touch raises {@code NoClassDefFoundError} - an {@code Error}, which {@code catch (Exception)}
     * does not hold. In the field that escaped the function and {@code /health} answered a raw 500
     * instead of a 503 the DevOps reader could act on. It must fail the check, and say so.
     */
    @Test
    void anAlreadyPoisonedKafkaClassFailsHealthInsteadOfEscapingAsAnError() {
        var health = probing(() -> {
            throw new NoClassDefFoundError(
                    "Could not initialize class org.apache.kafka.clients.consumer.ConsumerConfig");
        });
        Object answer = health.handleEvent(Map.of("type", "health"), null, 1);
        assertInstanceOf(EventEnvelope.class, answer,
                "an Error must not escape the function - /health has to render it as a status");
        EventEnvelope envelope = (EventEnvelope) answer;
        assertEquals(503, envelope.getStatus());
        String text = String.valueOf(((Map<?, ?>) envelope.getBody()).get("text"));
        assertTrue(text.contains("failed to initialize"), "name the real fault: " + text);
        assertFalse(text.contains("cluster is not reachable"),
                "this is a classpath/loader fault, not an outage: " + text);
    }

    /**
     * The mechanism itself, pinned against future {@code kafka-clients} upgrades.
     *
     * <p>{@code ConfigDef.define} resolves a {@code Type.CLASS} config's DEFAULT the moment the key is
     * defined, and {@code sasl.oauthbearer.jwt.retriever.class} defaults to a class NAME. So
     * initializing {@code ConsumerConfig} is a classloading event on its own - no broker, no SASL, no
     * credentials - and the thread that happens to do it first decides the outcome for the whole JVM.
     * A fresh copy of kafka-clients in an isolated loader is what makes that observable here: the real
     * {@code ConsumerConfig} was initialized by the first test to touch it and cannot be un-initialized.</p>
     */
    @Test
    void initializingAKafkaConfigClassResolvesClassDefaultsThroughTheContextClassLoader() throws Exception {
        try (URLClassLoader isolated = freshCopyOfKafkaClients()) {
            // an Error from a class initializer propagates straight out of Method.invoke - it is not
            // wrapped in InvocationTargetException, which is the same reason catch (Exception) misses it
            var failure = assertThrows(ExceptionInInitializerError.class,
                    () -> configNamesOn(isolated, /* blindContextClassLoader */ true));
            assertTrue(String.valueOf(failure.getCause().getMessage())
                            .contains("sasl.oauthbearer.jwt.retriever.class"),
                    "expected the class-valued config default to be the trigger, but got: "
                            + failure.getCause());

            // and it never heals: a perfectly ordinary thread with the right loader is refused too
            assertThrows(NoClassDefFoundError.class, () -> configNamesOn(isolated, false),
                    "one bad initialization poisons the class for the life of the JVM - which is why "
                            + "the loader has to be right the first time, not merely eventually");
        }
    }

    /** The positive control: the identical call succeeds when the loader is the module's own. */
    @Test
    void theSameInitializationSucceedsUnderTheModuleClassLoader() throws Exception {
        try (URLClassLoader isolated = freshCopyOfKafkaClients()) {
            assertTrue((int) configNamesSize(isolated) > 0,
                    "an ordinary loader must initialize ConsumerConfig without trouble - otherwise the "
                            + "test above proves nothing about the loader");
        }
    }

    /**
     * kafka-clients loaded again, parented to the platform loader so nothing delegates back to the
     * copy this JVM already initialized.
     */
    private static URLClassLoader freshCopyOfKafkaClients() {
        URL kafka = ConsumerConfig.class.getProtectionDomain().getCodeSource().getLocation();
        URL slf4j = Logger.class.getProtectionDomain().getCodeSource().getLocation();
        return new URLClassLoader(new URL[]{kafka, slf4j}, ClassLoader.getPlatformClassLoader());
    }

    private static void configNamesOn(URLClassLoader isolated, boolean blind) throws Exception {
        Class<?> consumerConfig =
                Class.forName("org.apache.kafka.clients.consumer.ConsumerConfig", false, isolated);
        Method configNames = consumerConfig.getMethod("configNames");
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader nothingVisible = new URLClassLoader(new URL[0], null)) {
            Thread.currentThread().setContextClassLoader(blind ? nothingVisible : isolated);
            configNames.invoke(null);
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    private static int configNamesSize(URLClassLoader isolated) throws Exception {
        Class<?> consumerConfig =
                Class.forName("org.apache.kafka.clients.consumer.ConsumerConfig", false, isolated);
        Method configNames = consumerConfig.getMethod("configNames");
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(isolated);
            return ((java.util.Set<?>) configNames.invoke(null)).size();
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }
}
