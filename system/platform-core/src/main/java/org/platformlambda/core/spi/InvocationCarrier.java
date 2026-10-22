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

package org.platformlambda.core.spi;

/**
 * The string tags of an event envelope, as an observer sees them: the carrier of the linkage between
 * hops. An observer reads and writes only its own keys; the envelope's body and headers are out of
 * reach. A tag is serialized with the envelope, so it survives every transport Mercury uses.
 */
public interface InvocationCarrier {

    /**
     * @param key a tag name
     * @return the tag's value, or null
     */
    String get(String key);

    /**
     * @param key a tag name owned by the observer
     * @param value the value; a null or empty value removes the tag
     */
    void set(String key, String value);

    /**
     * @param key a tag name owned by the observer
     */
    void remove(String key);
}
