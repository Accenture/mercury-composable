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

package org.platformlambda.contracts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.TreeMap;
import java.util.regex.Pattern;

/** Immutable, deterministic view of installed Mercury contract providers. */
public final class ContractRegistry {
    private static final Pattern PROVIDER_ID = Pattern.compile("[a-z][a-z0-9-]{1,63}");
    private static final int MAX_PROVIDERS = 32;
    private static final int MAX_CONTRACTS_PER_PROVIDER = 32;
    private final List<MercuryContractProvider> providers;
    private final Map<String, MercuryContract> contracts;

    private ContractRegistry(List<MercuryContractProvider> providers,
                             Map<String, MercuryContract> contracts) {
        this.providers = providers;
        this.contracts = contracts;
    }

    public static ContractRegistry load() {
        var loader = Thread.currentThread().getContextClassLoader();
        return load(loader == null ? ContractRegistry.class.getClassLoader() : loader);
    }

    public static ContractRegistry load(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader");
        try {
            var discovered = new ArrayList<MercuryContractProvider>();
            for (var provider : ServiceLoader.load(MercuryContractProvider.class, classLoader)) {
                if (discovered.size() == MAX_PROVIDERS) {
                    throw new ContractException(ContractError.CONTRACT_VERSION_MISMATCH);
                }
                discovered.add(provider);
            }
            return of(discovered);
        } catch (ServiceConfigurationError | RuntimeException e) {
            if (e instanceof ContractException contractException) {
                throw contractException;
            }
            throw new ContractException(ContractError.CONTRACT_VERSION_MISMATCH);
        }
    }

    public static ContractRegistry of(Collection<? extends MercuryContractProvider> candidates) {
        Objects.requireNonNull(candidates, "candidates");
        if (candidates.size() > MAX_PROVIDERS) {
            throw new ContractException(ContractError.CONTRACT_VERSION_MISMATCH);
        }
        var ordered = new ArrayList<MercuryContractProvider>();
        var providerIds = new java.util.HashSet<String>();
        var byId = new TreeMap<String, MercuryContract>();
        for (var provider : candidates) {
            if (provider == null) {
                throw new ContractException(ContractError.CONTRACT_VERSION_MISMATCH);
            }
            final String providerId;
            final String buildId;
            final Collection<MercuryContract> suppliedContracts;
            try {
                providerId = provider.providerId();
                buildId = provider.contractBuildId();
                suppliedContracts = provider.contracts();
            } catch (RuntimeException e) {
                throw new ContractException(ContractError.CONTRACT_VERSION_MISMATCH);
            }
            if (providerId == null || !PROVIDER_ID.matcher(providerId).matches()
                    || !providerIds.add(providerId) || !ContractBuild.ID.equals(buildId)
                    || suppliedContracts == null) {
                throw new ContractException(ContractError.CONTRACT_VERSION_MISMATCH);
            }
            var contracts = new ArrayList<MercuryContract>();
            for (var contract : suppliedContracts) {
                if (contract == null || contracts.size() == MAX_CONTRACTS_PER_PROVIDER
                        || byId.putIfAbsent(contract.id(), contract) != null) {
                    throw new ContractException(ContractError.CONTRACT_VERSION_MISMATCH);
                }
                contracts.add(contract);
            }
            ordered.add(new StableProvider(providerId, buildId, List.copyOf(contracts)));
        }
        ordered.sort((a, b) -> a.providerId().compareTo(b.providerId()));
        var stableProviders = Collections.unmodifiableList(ordered);
        var stableContracts = Collections.unmodifiableMap(new LinkedHashMap<>(byId));
        return new ContractRegistry(stableProviders, stableContracts);
    }

    public List<MercuryContractProvider> providers() {
        return providers;
    }

    public Collection<MercuryContract> contracts() {
        return contracts.values();
    }

    public MercuryContract get(String id) {
        return contracts.get(id);
    }

    private record StableProvider(String providerId, String contractBuildId,
                                  Collection<MercuryContract> contracts)
            implements MercuryContractProvider {
    }
}
