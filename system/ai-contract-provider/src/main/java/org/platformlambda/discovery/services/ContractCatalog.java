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

package org.platformlambda.discovery.services;

import org.platformlambda.core.util.ConfigReader;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.discovery.models.ContractEntry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The operational contract catalog, loaded from contracts.yaml and validated eagerly.
 * <p>
 * Anchor classes are declared as strings so this app does not need a runtime dependency
 * on every module it describes (a runtime dependency on the playground engine would
 * auto-preload its functions into this app). ContractCatalogTest resolves every anchor
 * with Class.forName using test-scope dependencies, so a renamed or removed behavior
 * class still fails the reactor build.
 */
public class ContractCatalog {
    private static final Pattern CONTRACT_ID = Pattern.compile("[a-z][a-z0-9-]{1,63}");
    private static final Pattern SUMMARY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9 .,;:/()_+\\-]{0,239}");
    private static final Pattern ANCHOR_CLASS = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z0-9_$]+)*");
    private static final ContractCatalog INSTANCE = new ContractCatalog();

    private final List<ContractEntry> contracts;

    private ContractCatalog() {
        this.contracts = load(new ConfigReader("classpath:/contracts.yaml"));
    }

    public static ContractCatalog getInstance() {
        return INSTANCE;
    }

    public List<ContractEntry> getContracts() {
        return contracts;
    }

    public ContractEntry getContract(String id) {
        return contracts.stream().filter(c -> c.id().equals(id)).findFirst().orElse(null);
    }

    @SuppressWarnings("unchecked")
    public static List<ContractEntry> load(ConfigReader config) {
        Object entries = config.get("contracts");
        if (!(entries instanceof List<?> list) || list.isEmpty()) {
            throw new IllegalArgumentException("contracts.yaml must contain a non-empty 'contracts' list");
        }
        var ids = new HashSet<String>();
        var result = new ArrayList<ContractEntry>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?>)) {
                throw new IllegalArgumentException("Each contract must be a map of id, module, summary, anchors, references");
            }
            var map = new MultiLevelMap((Map<String, Object>) item);
            var entry = new ContractEntry(
                    text(map, "id"), text(map, "module"), text(map, "summary"),
                    textList(map, "anchors"), textList(map, "references"));
            validate(entry, ids);
            result.add(entry);
        }
        result.sort((a, b) -> a.id().compareTo(b.id()));
        return List.copyOf(result);
    }

    private static void validate(ContractEntry entry, HashSet<String> ids) {
        if (!CONTRACT_ID.matcher(entry.id()).matches() || !ids.add(entry.id())) {
            throw new IllegalArgumentException("Invalid or duplicate contract id: " + entry.id());
        }
        if (!CONTRACT_ID.matcher(entry.module()).matches() || !SUMMARY.matcher(entry.summary()).matches()) {
            throw new IllegalArgumentException("Invalid module or summary for contract " + entry.id());
        }
        if (entry.anchors().isEmpty() || entry.anchors().size() > 16
                || entry.anchors().stream().anyMatch(a -> !ANCHOR_CLASS.matcher(a).matches())) {
            throw new IllegalArgumentException("Invalid behavior anchors for contract " + entry.id());
        }
        if (entry.references().isEmpty() || entry.references().size() > 32
                || entry.references().stream().anyMatch(r -> !r.startsWith("references/")
                    || r.contains("..") || r.contains("\\") || r.contains(":"))) {
            throw new IllegalArgumentException("Invalid references for contract " + entry.id());
        }
    }

    private static String text(MultiLevelMap map, String key) {
        return map.getElement(key) instanceof String value ? value : "";
    }

    private static List<String> textList(MultiLevelMap map, String key) {
        if (map.getElement(key) instanceof List<?> raw) {
            return List.copyOf(raw.stream().map(String::valueOf).toList());
        }
        return List.of();
    }
}
