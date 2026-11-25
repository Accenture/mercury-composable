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

package com.accenture.minigraph.start;

import com.accenture.minigraph.common.GraphModelGate;
import com.accenture.minigraph.common.GraphSet;
import com.accenture.minigraph.models.CompiledGraphs;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.ConfigReader;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

/**
 * Hot deploy and undeploy of graph sets (RFC-0017): a certified set deployed to a running engine, registered
 * through the deployment gate, and removed by name - without a restart.
 * <p>
 * The hot folder ({@code location.graph.deploy}, default {@code file:/tmp/deploy}) holds, per set, the package
 * {@code <id>.pack}, one unpacked {@code <graph-id>.json} per graph, and one engine-written record
 * {@code <id>.deployed.yaml} - the set's deployment record and its revision marker in one. A hot-deployable
 * set has exactly one main graph and is named after it, so the set name is the main graph's id and one string
 * is the handle everywhere. Two switches, both off by default, govern the folder: {@code graph.hot.deploy}
 * (a set may be deployed) and {@code graph.hot.undeploy} (a set may be removed); either on makes the engine
 * read the folder at startup and on demand.
 * <ul>
 * <li>a configured manifest owns its ids: a set that holds a bundled id is refused rather than shadowing it;
 *     one hot set owns an id; a re-upload of the same set name replaces the set all or none;</li>
 * <li>the registry is replaced in one swap per deployment, so a run never sees half a set;</li>
 * <li>the records are the allowlist: a package copied into the folder by hand is ignored with a warning;</li>
 * <li>every other instance converges on demand: a registry lookup for a hot-deployed id reads the set's record
 *     and reconciles that set when the record changed or is gone, and a lookup for an unknown id checks whether a
 *     record of that name exists - no poll, no broadcast, no global file.</li>
 * </ul>
 * The REST entries that call {@link #deploy}, {@link #undeploy} and {@link #list} are the next work package of the
 * design; this class is the engine side they share with startup.
 */
public final class HotDeployment {
    private static final Logger log = LoggerFactory.getLogger(HotDeployment.class);
    private static final Utility util = Utility.getInstance();
    public static final String DEPLOY_KEY = "graph.hot.deploy";
    public static final String UNDEPLOY_KEY = "graph.hot.undeploy";
    static final String LOCATION_KEY = "location.graph.deploy";
    static final String THROTTLE_KEY = "graph.hot.deploy.throttle";
    static final String DEFAULT_LOCATION = "file:/tmp/deploy";
    static final String RECORD_EXTENSION = ".deployed.yaml";
    private static final String JSON_EXT = ".json";
    private static final String FILE = "file:";
    private static final String FILE_PREFIX = "file:/";
    private static final String STAGING_PREFIX = ".staging-";
    private static final String DEFAULT_TEMP_DIR = "/tmp/graph";
    private static final String NOT_DEPLOYED = "not deployed - ";
    private static final String OFF_FOR_THIS_RUN = "Hot deployment is OFF for this run - ";
    private static final String CANNOT_WRITE = "Unable to write the hot deployment folder - ";
    private static final String DEPLOYED = "deployed";
    private static final String SET_FIELD = GraphSet.SET;
    private static final String VERSION_FIELD = "version";
    private static final String SHA256_FIELD = "sha256";
    private static final String DEPLOYED_AT_FIELD = "deployed_at";
    private static final String GRAPHS_FIELD = "graphs";
    private static final String FILES_FIELD = "files";
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);
    private static final AtomicReference<Settings> CONFIG = new AtomicReference<>(Settings.OFF);
    // deploy, undeploy and every reconcile serialize here; lookups read the maps below without it
    // (a ReentrantLock, not synchronized: a virtual thread must not pin its carrier)
    private static final ReentrantLock LOCK = new ReentrantLock();
    private static final ConcurrentMap<String, Installed> BY_SET = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, String> REFUSALS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Long> LAST_CHECK = new ConcurrentHashMap<>();

    private HotDeployment() {
        // utility class
    }

    /**
     * The folder's configuration for this run
     *
     * @param deploy the graph.hot.deploy switch
     * @param undeploy the graph.hot.undeploy switch
     * @param location the folder as configured (file:/...)
     * @param folder the folder as a path, or null when both switches are off
     * @param throttleMs the least time between two record reads for one set, 0 for every lookup
     */
    record Settings(boolean deploy, boolean undeploy, String location, Path folder, long throttleMs) {
        static final Settings OFF = new Settings(false, false, DEFAULT_LOCATION, null, 0);

        boolean enabled() {
            return folder != null;
        }
    }

    /**
     * A set registered from the hot folder on this instance
     *
     * @param sha256 the package's digest, as the record vouches for it
     * @param version the set's version field
     * @param graphs the graph ids the set holds
     * @param recordBytes the record as read when the set was registered, for the cheap "unchanged" check
     */
    private record Installed(String sha256, String version, List<String> graphs, String deployedAt,
                             byte[] recordBytes) {
    }

    /**
     * A set's record as read from {@code <id>.deployed.yaml}
     */
    private record DeployRecord(String set, String version, String sha256, String deployedAt, List<String> graphs,
                                List<String> files) {
    }

    /**
     * The answer of a deploy or an undeploy
     *
     * @param set the set name, which is its main graph's id
     * @param version the set's version field, empty when the set has none
     * @param sha256 the package's digest
     * @param graphs the graph ids the set holds (deploy) or held (undeploy)
     * @param deployedAt the record's time
     * @param replaced the previous version when the set was already deployed, or null
     * @param changed false when the same bytes were already deployed and nothing moved
     */
    public record Deployment(String set, String version, String sha256, List<String> graphs, String deployedAt,
                             Replaced replaced, boolean changed) {
    }

    /**
     * The version a deployment replaced
     *
     * @param version the previous version field
     * @param sha256 the previous package's digest
     */
    public record Replaced(String version, String sha256) {
    }

    /**
     * One row of the list of deployed sets
     *
     * @param set the set name
     * @param version the version field
     * @param sha256 the package's digest the record vouches for
     * @param deployedAt the record's time
     * @param graphs the graph ids
     * @param status "deployed", or "not deployed - " and the reason on this instance
     */
    public record SetStatus(String set, String version, String sha256, String deployedAt, List<String> graphs,
                            String status) {
    }

    /**
     * A deploy or undeploy that was refused, with the HTTP status the REST entry answers and every reason.
     */
    public static final class Refused extends Exception {
        private final int status;
        private final transient List<String> reasons;

        Refused(int status, List<String> reasons) {
            super(String.join("; ", reasons));
            this.status = status;
            this.reasons = List.copyOf(reasons);
        }

        Refused(int status, String reason) {
            this(status, List.of(reason));
        }

        public int getStatus() {
            return status;
        }

        public List<String> getReasons() {
            return reasons;
        }
    }

    /**
     * Configure from the application's properties: the two switches, the folder and the throttle. Either switch
     * on makes the folder live: it must be a writable file:/ folder outside the Playground's temporary folder.
     * A folder that cannot be used turns hot deployment off for this run, with an error.
     */
    static void configure() {
        var config = AppConfigReader.getInstance();
        var deploy = "true".equalsIgnoreCase(config.getProperty(DEPLOY_KEY, "false").trim());
        var undeploy = "true".equalsIgnoreCase(config.getProperty(UNDEPLOY_KEY, "false").trim());
        var location = config.getProperty(LOCATION_KEY, DEFAULT_LOCATION).trim();
        var throttle = Math.max(0, util.str2long(config.getProperty(THROTTLE_KEY, "0").trim()));
        configure(deploy, undeploy, location, throttle);
    }

    /**
     * Configure explicitly (the test seam of {@link #configure()}); a new configuration forgets every set this
     * instance had registered from the previous folder.
     *
     * @param deploy the graph.hot.deploy switch
     * @param undeploy the graph.hot.undeploy switch
     * @param location the folder as configured (file:/...)
     * @param throttleMs the least time between two record reads for one set
     */
    static void configure(boolean deploy, boolean undeploy, String location, long throttleMs) {
        LOCK.lock();
        try {
            BY_SET.clear();
            REFUSALS.clear();
            LAST_CHECK.clear();
            if (!deploy && !undeploy) {
                CONFIG.set(Settings.OFF);
                CompiledGraphs.setRefresh(null);
                return;
            }
            var problem = checkFolder(location);
            if (problem != null) {
                log.error("{}{}", OFF_FOR_THIS_RUN, problem);
                CONFIG.set(Settings.OFF);
                CompiledGraphs.setRefresh(null);
                return;
            }
            var folder = GraphSetLoader.folderOf(location);
            CONFIG.set(new Settings(deploy, undeploy, location, folder, throttleMs));
            CompiledGraphs.setRefresh(HotDeployment::check);
            log.info("Hot deployment folder ({}) - {}, deploy={}, undeploy={}", LOCATION_KEY, location, deploy,
                    undeploy);
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Return to the application's own configuration, forgetting every set a test registered from another folder.
     */
    static void reset() {
        configure();
    }

    private static String checkFolder(String location) {
        if (!location.startsWith(FILE_PREFIX)) {
            return LOCATION_KEY + " must be a file:/ folder the application can write, not " + location;
        }
        var folder = GraphSetLoader.folderOf(location);
        var temp = GraphSetLoader.folderOf(
                AppConfigReader.getInstance().getProperty("location.graph.temp", DEFAULT_TEMP_DIR));
        if (folder.startsWith(temp) || temp.startsWith(folder)) {
            return LOCATION_KEY + " must not be the Playground's temporary folder (location.graph.temp), inside it"
                    + " or above it";
        }
        try {
            Files.createDirectories(folder);
            var probe = Files.createTempFile(folder, ".probe-", ".tmp");
            Files.delete(probe);
            return null;
        } catch (IOException | SecurityException e) {
            return "cannot write in " + location + " - " + (e.getMessage() == null? e.getClass().getSimpleName() :
                    e.getMessage());
        }
    }

    /**
     * @return true when either switch is on and the folder is usable
     */
    public static boolean isEnabled() {
        return CONFIG.get().enabled();
    }

    /**
     * @return true when a set may be deployed through this instance
     */
    public static boolean canDeploy() {
        var settings = CONFIG.get();
        return settings.enabled() && settings.deploy();
    }

    /**
     * @return true when a set may be undeployed through this instance
     */
    public static boolean canUndeploy() {
        var settings = CONFIG.get();
        return settings.enabled() && settings.undeploy();
    }

    /**
     * @return the hot folder as a path, or null when hot deployment is off
     */
    static Path folder() {
        return CONFIG.get().folder();
    }

    /**
     * @return the hot folder as configured (file:/...)
     */
    public static String location() {
        return CONFIG.get().location();
    }

    /**
     * Whether a graph is a private subgraph of a hot-deployed set - any graph of such a set but its main graph.
     * A private subgraph is reached from its own set through graph.extension and from nowhere else.
     *
     * @param graphId the graph id
     * @return true for a private subgraph
     */
    public static boolean isPrivateSubgraph(String graphId) {
        var settings = CONFIG.get();
        if (!settings.enabled() || !settings.location().equals(CompiledGraphs.getGraphLocation(graphId))) {
            return false;
        }
        var set = CompiledGraphs.getGraphSet(graphId);
        return set != null && !set.name().equals(graphId);
    }

    /**
     * Read the folder at startup, after the configured manifests: every set a record names is registered through
     * the gate, a package no record names is noted, and a folder that another manifest unpacks into is refused.
     */
    static void reconcileAll() {
        var settings = CONFIG.get();
        if (!settings.enabled()) {
            return;
        }
        if (GraphSetLoader.meetsUnpackFolder(settings.folder())) {
            log.error("{}{} {} is a manifest's 'unpack' folder, inside one or above one", OFF_FOR_THIS_RUN,
                    LOCATION_KEY, settings.location());
            CONFIG.set(Settings.OFF);
            CompiledGraphs.setRefresh(null);
            return;
        }
        CompiledGraphs.addDeployedLocation(settings.location());
        LOCK.lock();
        try {
            removeStaging(settings.folder());
            var sets = recordedSets(settings.folder());
            for (var id : sets) {
                reconcile(id);
            }
            for (var stray : strayPackages(settings.folder(), sets)) {
                log.warn("Package {} in {} has no record and is ignored - a deployment goes through the deploy"
                        + " endpoint", stray, settings.location());
            }
            log.info("Hot deployment folder {} - {} deployed", settings.location(), count(BY_SET.size()));
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * The refresh hook: a lookup for a hot-deployed id reads its set's record and reconciles the set when the
     * record changed or is gone; a lookup for an unknown id checks whether a record of that name exists and loads
     * the set if it does. A manifest's id costs nothing.
     *
     * @param graphId the id looked up
     * @param location the deployed location of the compiled copy, or null for an unknown id
     */
    static void check(String graphId, String location) {
        var settings = CONFIG.get();
        if (!settings.enabled()) {
            return;
        }
        if (location == null) {
            if (GraphModelGate.isValidGraphId(graphId) && !throttled(settings, graphId)
                    && Files.isRegularFile(recordPath(settings.folder(), graphId))) {
                reconcileUnderLock(graphId);
            }
            return;
        }
        if (!settings.location().equals(location)) {
            return;
        }
        var set = CompiledGraphs.getGraphSet(graphId);
        if (set == null || throttled(settings, set.name())) {
            return;
        }
        var installed = BY_SET.get(set.name());
        if (installed == null || !Arrays.equals(installed.recordBytes(), readBytes(recordPath(settings.folder(),
                set.name())))) {
            reconcileUnderLock(set.name());
        }
    }

    private static boolean throttled(Settings settings, String setName) {
        if (settings.throttleMs() <= 0) {
            return false;
        }
        var now = System.currentTimeMillis();
        var last = LAST_CHECK.get(setName);
        if (last != null && now - last < settings.throttleMs()) {
            return true;
        }
        LAST_CHECK.put(setName, now);
        return false;
    }

    private static void reconcileUnderLock(String setName) {
        LOCK.lock();
        try {
            reconcile(setName);
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Bring one set to the state its record describes - the caller holds the lock.
     * <ol>
     * <li>no record: a set registered from this folder leaves the registry (another instance undeployed it);</li>
     * <li>the record and the package agree, and the registered copy came from the same bytes: nothing;</li>
     * <li>the record and the package disagree: the record is read once more, and a mismatch that persists
     *     unregisters the set with an error - the bytes are not the bytes the record vouches for;</li>
     * <li>otherwise the package is loaded, unpacked, gated and registered as a unit, in one swap.</li>
     * </ol>
     */
    private static void reconcile(String setName) {
        var settings = CONFIG.get();
        var recordFile = recordPath(settings.folder(), setName);
        var recordBytes = readBytes(recordFile);
        if (recordBytes == null) {
            if (BY_SET.containsKey(setName)) {
                unregister(setName, settings);
                log.info("Set {} undeployed elsewhere - its record is gone", setName);
            }
            REFUSALS.remove(setName);
            return;
        }
        var installed = BY_SET.get(setName);
        if (installed != null && Arrays.equals(installed.recordBytes(), recordBytes)) {
            return;
        }
        var agreed = agreedRecord(settings, setName, recordFile, recordBytes);
        if (agreed == null) {
            return;
        }
        if (installed != null && installed.sha256().equals(agreed.sha256())) {
            // the record was rewritten with the same digest - nothing to load
            BY_SET.put(setName, new Installed(installed.sha256(), installed.version(), installed.graphs(),
                    agreed.deployRecord().deployedAt(), agreed.recordBytes()));
            return;
        }
        try {
            var contents = readSet(setName, agreed.packBytes());
            checkOwnership(setName, contents, settings);
            install(setName, contents, agreed, settings);
        } catch (Refused e) {
            refuse(setName, e.getMessage());
        }
    }

    /**
     * A record and a package that agree on the digest
     */
    private record Agreed(DeployRecord deployRecord, byte[] recordBytes, byte[] packBytes, String sha256) {
    }

    private static Agreed agreedRecord(Settings settings, String setName, Path recordFile, byte[] firstBytes) {
        var attempt = agreement(settings, setName, firstBytes);
        if (attempt == null) {
            // a deploy may have moved the package before its record: once more, from the file
            attempt = agreement(settings, setName, readBytes(recordFile));
        }
        if (attempt == null) {
            if (BY_SET.containsKey(setName)) {
                unregister(setName, settings);
            }
            refuse(setName, "the package does not match its record");
        }
        return attempt;
    }

    private static Agreed agreement(Settings settings, String setName, byte[] recordBytes) {
        if (recordBytes == null) {
            return null;
        }
        var deployRecord = parseRecord(recordPath(settings.folder(), setName));
        if (deployRecord == null || !setName.equals(deployRecord.set()) || deployRecord.sha256() == null) {
            return null;
        }
        var packBytes = readBytes(settings.folder().resolve(setName + GraphSet.EXTENSION));
        if (packBytes == null) {
            return null;
        }
        var sha256 = GraphSetLoader.sha256(packBytes);
        return sha256.equals(deployRecord.sha256())? new Agreed(deployRecord, recordBytes, packBytes, sha256) : null;
    }

    private static void refuse(String setName, String reason) {
        REFUSALS.put(setName, reason);
        log.error("Set {} {}{}", setName, NOT_DEPLOYED, reason);
    }

    private static void unregister(String setName, Settings settings) {
        CompiledGraphs.replaceGraphs(CompiledGraphs.getGraphsOfSet(setName, settings.location()), Map.of(),
                settings.location(), null);
        BY_SET.remove(setName);
    }

    /**
     * Deploy a set from the bytes of its package: the strict read, the main-graph rule, the ownership rule, the
     * files staged and gated, then one swap of the registry and the record written last.
     *
     * @param bytes the package
     * @param expectedSha256 the digest the caller expects (the field's ledger), or null
     * @return the deployment
     * @throws Refused with the HTTP status and every reason when the set is not deployed
     */
    public static Deployment deploy(byte[] bytes, String expectedSha256) throws Refused {
        var settings = CONFIG.get();
        if (!settings.enabled() || !settings.deploy()) {
            throw new Refused(503, "hot deploy is off on this engine (" + DEPLOY_KEY + ")");
        }
        if (bytes == null || bytes.length == 0) {
            throw new Refused(400, "The request body is the graph set (.pack) to deploy, sent as"
                    + " application/octet-stream");
        }
        var sha256 = GraphSetLoader.sha256(bytes);
        if (expectedSha256 != null && !expectedSha256.trim().equalsIgnoreCase(sha256)) {
            throw new Refused(400, "SHA-256 does not match - the body's digest is " + sha256);
        }
        var contents = readSet(null, bytes);
        var setName = contents.manifest().get(SET_FIELD);
        LOCK.lock();
        try {
            checkOwnership(setName, contents, settings);
            var installed = BY_SET.get(setName);
            if (installed != null && installed.sha256().equals(sha256)) {
                return new Deployment(setName, installed.version(), sha256, installed.graphs(),
                        installed.deployedAt(), null, false);
            }
            var version = contents.manifest().getOrDefault(VERSION_FIELD, "");
            var ids = new ArrayList<>(contents.graphs().keySet());
            var deployedAt = TIMESTAMP.format(Instant.now());
            var files = new ArrayList<String>();
            files.add(setName + GraphSet.EXTENSION);
            ids.forEach(id -> files.add(id + JSON_EXT));
            var recordText = recordText(new DeployRecord(setName, version, sha256, deployedAt, ids, files));
            var recordBytes = recordText.getBytes(StandardCharsets.UTF_8);
            var agreed = new Agreed(new DeployRecord(setName, version, sha256, deployedAt, ids, files), recordBytes,
                    bytes, sha256);
            var replaced = installed == null? null : new Replaced(installed.version(), installed.sha256());
            install(setName, contents, agreed, settings);
            writeAtomically(recordPath(settings.folder(), setName), recordBytes, settings.folder());
            log.info("Deployed set {}{} - {} into {} (SHA-256 {})", setName,
                    version.isEmpty()? "" : " (version " + version + ")", count(ids.size()), settings.location(),
                    sha256);
            return new Deployment(setName, version, sha256, List.copyOf(ids), deployedAt, replaced, true);
        } catch (IOException e) {
            throw new Refused(500, CANNOT_WRITE + e.getMessage());
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Undeploy a set by its main graph's id: its graphs leave the registry in one swap, the record is deleted
     * first and then the files it named.
     *
     * @param graphId the set name, which is its main graph's id
     * @return the set as removed
     * @throws Refused 404 when the folder holds no such set
     */
    public static Deployment undeploy(String graphId) throws Refused {
        var settings = CONFIG.get();
        if (!settings.enabled() || !settings.undeploy()) {
            throw new Refused(503, "hot undeploy is off on this engine (" + UNDEPLOY_KEY + ")");
        }
        if (graphId == null || !GraphModelGate.isValidGraphId(graphId)) {
            throw new Refused(400, "a set name uses letters, digits, '_' and '-' only");
        }
        LOCK.lock();
        try {
            var recordFile = recordPath(settings.folder(), graphId);
            var deployRecord = parseRecord(recordFile);
            if (deployRecord == null) {
                throw new Refused(404, "set " + graphId + " is not deployed in " + settings.location());
            }
            var graphs = CompiledGraphs.getGraphsOfSet(graphId, settings.location());
            unregister(graphId, settings);
            REFUSALS.remove(graphId);
            Files.deleteIfExists(recordFile);
            for (var name : deployRecord.files()) {
                deleteOwnFile(settings.folder(), name);
            }
            log.info("Undeployed set {} - {} from {}", graphId, count(graphs.size()), settings.location());
            var ids = graphs.isEmpty()? deployRecord.graphs() : graphs;
            return new Deployment(graphId, deployRecord.version(), deployRecord.sha256(), List.copyOf(ids),
                    deployRecord.deployedAt(), null, true);
        } catch (IOException e) {
            throw new Refused(500, "Unable to remove the set's files - " + e.getMessage());
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * The deployed sets as the folder records them, each reconciled first, with its status on this instance.
     *
     * @return the rows, by set name
     */
    public static List<SetStatus> list() {
        var settings = CONFIG.get();
        List<SetStatus> result = new ArrayList<>();
        if (!settings.enabled()) {
            return result;
        }
        LOCK.lock();
        try {
            for (var setName : recordedSets(settings.folder())) {
                reconcile(setName);
                var deployRecord = parseRecord(recordPath(settings.folder(), setName));
                if (deployRecord != null) {
                    var status = BY_SET.containsKey(setName)? DEPLOYED :
                            NOT_DEPLOYED + REFUSALS.getOrDefault(setName, "not registered on this instance");
                    result.add(new SetStatus(setName, deployRecord.version(), deployRecord.sha256(),
                            deployRecord.deployedAt(), deployRecord.graphs(), status));
                }
            }
        } finally {
            LOCK.unlock();
        }
        return result;
    }

    private static GraphSet.Contents readSet(String expectedName, byte[] bytes) throws Refused {
        final GraphSet.Contents contents;
        try {
            contents = GraphSet.read(bytes);
        } catch (GraphSet.RefusedException e) {
            throw new Refused(400, e.getReasons());
        } catch (IOException e) {
            throw new Refused(400, "Not a graph set - " + e.getMessage());
        }
        var setName = contents.manifest().get(SET_FIELD);
        if (setName == null || !GraphModelGate.isValidGraphId(setName)) {
            throw new Refused(400, "manifest field '" + SET_FIELD + "' - a set name uses letters, digits, '_' and"
                    + " '-' only");
        }
        if (expectedName != null && !expectedName.equals(setName)) {
            throw new Refused(400, "the package is set '" + setName + "', not '" + expectedName + "'");
        }
        // a hot-deployable set has exactly one main graph and is named after it (D14)
        if (!contents.graphs().containsKey(setName)) {
            throw new Refused(400, "a hot-deployable set is named after its main graph - the set holds no graph '"
                    + setName + "'");
        }
        var entryPoint = contents.manifest().get(GraphSet.GRAPH_ID);
        if (entryPoint != null && !entryPoint.equals(setName)) {
            throw new Refused(400, "manifest field '" + GraphSet.GRAPH_ID + "' is '" + entryPoint
                    + "' - it must name the main graph, which is the set name '" + setName + "'");
        }
        return contents;
    }

    /**
     * A configured manifest owns its ids, and one hot set owns an id (D4): a graph the set holds may be new, or
     * owned by this set's previous version in the hot folder, and nothing else.
     */
    private static void checkOwnership(String setName, GraphSet.Contents contents, Settings settings)
            throws Refused {
        List<String> reasons = new ArrayList<>();
        for (var id : contents.graphs().keySet()) {
            var owner = CompiledGraphs.getGraphLocation(id);
            if (owner == null) {
                continue;
            }
            var set = CompiledGraphs.getGraphSet(id);
            var ownVersion = settings.location().equals(owner) && set != null && setName.equals(set.name());
            if (!ownVersion) {
                reasons.add("graph id " + id + " is owned by " + GraphSetLoader.describe(owner, set));
            }
        }
        if (!reasons.isEmpty()) {
            throw new Refused(409, reasons);
        }
    }

    /**
     * Stage the set's files in a folder of their own, gate them there, then move them into place and swap the
     * registry - all or none. The record is the caller's to write (a deploy) or to leave alone (a reconcile).
     */
    private static void install(String setName, GraphSet.Contents contents, Agreed agreed, Settings settings)
            throws Refused {
        var folder = settings.folder();
        var ids = new ArrayList<>(contents.graphs().keySet());
        Path staging = null;
        try {
            staging = Files.createTempDirectory(folder, STAGING_PREFIX);
            Files.write(staging.resolve(setName + GraphSet.EXTENSION), agreed.packBytes());
            for (var graph : contents.graphs().entrySet()) {
                Files.writeString(staging.resolve(graph.getKey() + JSON_EXT), GraphSet.toJson(graph.getValue()));
            }
            List<String> failures = new ArrayList<>();
            var models = GraphSetLoader.gate(FILE + staging, ids, failures);
            if (!failures.isEmpty()) {
                throw new Refused(400, failures);
            }
            try (Stream<Path> staged = Files.list(staging)) {
                for (var file : staged.toList()) {
                    Files.move(file, folder.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                }
            }
            var previous = CompiledGraphs.getGraphsOfSet(setName, settings.location());
            var version = agreed.deployRecord().version();
            CompiledGraphs.replaceGraphs(previous, models, settings.location(),
                    new CompiledGraphs.DeployedSet(setName, version));
            for (var id : previous) {
                if (!contents.graphs().containsKey(id)) {
                    deleteOwnFile(folder, id + JSON_EXT);
                }
            }
            BY_SET.put(setName, new Installed(agreed.sha256(), version, List.copyOf(ids),
                    agreed.deployRecord().deployedAt(), agreed.recordBytes()));
            REFUSALS.remove(setName);
            LAST_CHECK.put(setName, System.currentTimeMillis());
        } catch (IOException e) {
            throw new Refused(500, CANNOT_WRITE + e.getMessage());
        } finally {
            if (staging != null) {
                deleteTree(staging);
            }
        }
    }

    private static void deleteOwnFile(Path folder, String name) throws IOException {
        // a name becomes a path only when it is a set's package or a graph's file, inside the folder
        var dot = name.lastIndexOf('.');
        var stem = dot == -1? name : name.substring(0, dot);
        var valid = GraphModelGate.isValidGraphId(stem)
                && (name.endsWith(JSON_EXT) || name.endsWith(GraphSet.EXTENSION));
        if (valid) {
            var file = folder.resolve(name).normalize();
            if (file.getParent().equals(folder)) {
                Files.deleteIfExists(file);
            }
        }
    }

    private static Set<String> recordedSets(Path folder) {
        Set<String> result = new TreeSet<>();
        try (Stream<Path> files = Files.list(folder)) {
            for (var file : files.toList()) {
                var name = file.getFileName().toString();
                if (name.endsWith(RECORD_EXTENSION) && Files.isRegularFile(file)) {
                    var setName = name.substring(0, name.length() - RECORD_EXTENSION.length());
                    if (GraphModelGate.isValidGraphId(setName)) {
                        result.add(setName);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Unable to read the hot deployment folder {} - {}", folder, e.getMessage());
        }
        return result;
    }

    private static List<String> strayPackages(Path folder, Set<String> recorded) {
        List<String> result = new ArrayList<>();
        try (Stream<Path> files = Files.list(folder)) {
            for (var file : files.toList()) {
                var name = file.getFileName().toString();
                if (name.endsWith(GraphSet.EXTENSION)
                        && !recorded.contains(name.substring(0, name.length() - GraphSet.EXTENSION.length()))) {
                    result.add(name);
                }
            }
        } catch (IOException e) {
            log.error("Unable to read the hot deployment folder {} - {}", folder, e.getMessage());
        }
        return result;
    }

    private static void removeStaging(Path folder) {
        try (Stream<Path> files = Files.list(folder)) {
            for (var file : files.toList()) {
                if (Files.isDirectory(file) && file.getFileName().toString().startsWith(STAGING_PREFIX)) {
                    deleteTree(file);
                }
            }
        } catch (IOException e) {
            log.warn("Unable to clean up the hot deployment folder {} - {}", folder, e.getMessage());
        }
    }

    private static void deleteTree(Path dir) {
        try (Stream<Path> files = Files.walk(dir)) {
            for (var file : files.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            log.warn("Unable to remove {} - {}", dir, e.getMessage());
        }
    }

    static Path recordPath(Path folder, String setName) {
        return folder.resolve(setName + RECORD_EXTENSION);
    }

    private static byte[] readBytes(Path file) {
        try {
            return Files.isRegularFile(file)? Files.readAllBytes(file) : null;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * The record's text: one set, written by the engine and read by every instance
     */
    private static String recordText(DeployRecord deployRecord) {
        var sb = new StringBuilder();
        sb.append("# Written by the hot-deploy functions - rewritten at every change of this set; do not edit\n");
        sb.append(SET_FIELD).append(": ").append(quoted(deployRecord.set())).append('\n');
        sb.append(VERSION_FIELD).append(": ").append(quoted(deployRecord.version())).append('\n');
        sb.append(SHA256_FIELD).append(": ").append(quoted(deployRecord.sha256())).append('\n');
        sb.append(DEPLOYED_AT_FIELD).append(": ").append(quoted(deployRecord.deployedAt())).append('\n');
        appendList(sb, GRAPHS_FIELD, deployRecord.graphs());
        appendList(sb, FILES_FIELD, deployRecord.files());
        return sb.toString();
    }

    private static void appendList(StringBuilder sb, String field, Collection<String> values) {
        sb.append(field).append(':').append(values.isEmpty()? " []\n" : "\n");
        values.forEach(value -> sb.append("  - ").append(quoted(value)).append('\n'));
    }

    private static String quoted(String text) {
        return "'" + (text == null? "" : text.replace("'", "''")) + "'";
    }

    /**
     * A set's record as the file holds it, or null when there is none or it cannot be read
     */
    private static DeployRecord parseRecord(Path file) {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            var reader = new ConfigReader(FILE + file);
            return new DeployRecord(reader.getProperty(SET_FIELD), reader.getProperty(VERSION_FIELD, ""),
                    reader.getProperty(SHA256_FIELD), reader.getProperty(DEPLOYED_AT_FIELD, ""),
                    strings(reader.get(GRAPHS_FIELD)), strings(reader.get(FILES_FIELD)));
        } catch (IllegalArgumentException e) {
            log.error("Unable to read the deployment record {} - {}", file, e.getMessage());
            return null;
        }
    }

    private static List<String> strings(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (var item : list) {
                result.add(String.valueOf(item));
            }
        }
        return result;
    }

    private static void writeAtomically(Path target, byte[] bytes, Path folder) throws IOException {
        var temp = Files.createTempFile(folder, ".record-", ".tmp");
        try {
            Files.write(temp, bytes);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static String count(int n) {
        return n + (n == 1? " set" : " sets");
    }
}
