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

package org.platformlambda.sync;

import org.platformlambda.core.serializers.SimpleMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * One entry of a {@code queue:{cid}} rendezvous queue - the unit both the streaming and the one-shot
 * return paths store and drain. Serialized as compact JSON, e.g.
 * {@code {"type":"data","name":"tokens","body":"..."}}; {@code name} (an optional SSE event name) and
 * {@code body} are omitted when null.
 *
 * <p>The {@code eof} and {@code exception} types are {@link #isTerminal() terminal}: "the end signal is
 * also an event", stored like any other segment, so it can never outrun the data it follows. A one-shot
 * response is the degenerate case - a queue whose first entry is terminal.</p>
 *
 * <p>There is deliberately no sequence number: ordering, where required, is the producer's posting
 * discipline (post sequentially over one connection), and reads are destructive pops, so neither side
 * keeps an index. An application that wants its own ordinal is free to stamp one inside {@code body}.</p>
 *
 * @param type {@link #DATA}, {@link #EOF} or {@link #EXCEPTION}
 * @param name optional SSE event name forwarded to the {@code x-event-stream} edge; null = unnamed
 * @param body the segment payload as text (applications put JSON inside if they wish); null = empty
 */
public record StreamSegment(String type, String name, String body) {

    public static final String DATA = "data";
    public static final String EOF = "eof";
    public static final String EXCEPTION = "exception";

    private static final Set<String> VALID_TYPES = Set.of(DATA, EOF, EXCEPTION);
    private static final String TYPE_FIELD = "type";
    private static final String NAME_FIELD = "name";
    private static final String BODY_FIELD = "body";

    /**
     * Validating factory used by the producer paths.
     *
     * @throws IllegalArgumentException if {@code type} is not {@code data}, {@code eof} or {@code exception}
     */
    public static StreamSegment of(String type, String name, String body) {
        if (!VALID_TYPES.contains(type)) {
            throw new IllegalArgumentException("Segment type must be one of " + VALID_TYPES + ", not " + type);
        }
        return new StreamSegment(type, name, body);
    }

    /** @return true for the {@code eof} and {@code exception} entries that complete a rendezvous. */
    public boolean isTerminal() {
        return EOF.equals(type) || EXCEPTION.equals(type);
    }

    /** Serialize to the compact JSON wire form (null fields omitted; every byte rides the hot path). */
    public String toJson() {
        Map<String, String> wire = new LinkedHashMap<>();
        wire.put(TYPE_FIELD, type);
        if (name != null) {
            wire.put(NAME_FIELD, name);
        }
        if (body != null) {
            wire.put(BODY_FIELD, body);
        }
        return SimpleMapper.getInstance().getCompactGson().toJson(wire);
    }

    /**
     * Parse a stored queue entry.
     *
     * @throws IllegalArgumentException if the text is not a segment (not JSON, or an unknown type)
     */
    @SuppressWarnings("unchecked")
    public static StreamSegment fromJson(String json) {
        final Map<String, Object> wire;
        try {
            wire = SimpleMapper.getInstance().getMapper().readValue(json, Map.class);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Not a stream segment: " + json, e);
        }
        Object type = wire.get(TYPE_FIELD);
        if (!(type instanceof String t) || !VALID_TYPES.contains(t)) {
            throw new IllegalArgumentException("Segment type must be one of " + VALID_TYPES + ", not " + type);
        }
        Object name = wire.get(NAME_FIELD);
        Object body = wire.get(BODY_FIELD);
        return new StreamSegment(t, name == null ? null : String.valueOf(name),
                body == null ? null : String.valueOf(body));
    }
}
