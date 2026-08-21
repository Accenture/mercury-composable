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

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** A behavior contract contributed by one installed Mercury module. */
public record MercuryContract(
        String id,
        String module,
        String summary,
        List<Class<?>> behaviorAnchors,
        List<String> references) {

    private static final Pattern ID_PATTERN = Pattern.compile("[a-z][a-z0-9-]{1,63}");
    private static final Pattern SUMMARY_PATTERN = Pattern.compile(
            "[A-Za-z0-9][A-Za-z0-9 .,;:/()_+\\-]{0,239}");

    public MercuryContract {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(module, "module");
        Objects.requireNonNull(summary, "summary");
        Objects.requireNonNull(behaviorAnchors, "behaviorAnchors");
        Objects.requireNonNull(references, "references");
        if (!ID_PATTERN.matcher(id).matches() || !ID_PATTERN.matcher(module).matches()
                || !SUMMARY_PATTERN.matcher(summary).matches() || behaviorAnchors.isEmpty()
                || behaviorAnchors.size() > 16 || references.isEmpty() || references.size() > 32) {
            throw new IllegalArgumentException("Invalid Mercury contract");
        }
        behaviorAnchors = List.copyOf(behaviorAnchors);
        references = List.copyOf(references);
        if (behaviorAnchors.stream().anyMatch(Objects::isNull)
                || references.stream().anyMatch(path -> path == null
                || path.length() > 256 || !path.startsWith("references/") || path.contains("..")
                || path.contains("\\") || path.contains(":"))) {
            throw new IllegalArgumentException("Invalid Mercury contract inventory");
        }
    }
}
