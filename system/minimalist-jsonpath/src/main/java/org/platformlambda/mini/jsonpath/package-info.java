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

/**
 * A minimalist JSONPath engine written from <a href="https://www.rfc-editor.org/rfc/rfc9535">RFC 9535</a>, the IETF
 * JSONPath standard.
 * <p>
 * {@link org.platformlambda.mini.jsonpath.JsonPath#compile(java.lang.String)} parses a query into an immutable,
 * thread-safe form, refusing any query outside the standard's grammar or type rules with a
 * {@link org.platformlambda.mini.jsonpath.JsonPathException} that names the reason and the position.
 * {@link org.platformlambda.mini.jsonpath.JsonPath#select(java.lang.Object)} evaluates it over a tree of
 * {@link java.util.Map} and {@link java.util.List}, with no conversion step. The five standard functions are
 * supported - {@code length}, {@code count}, {@code match}, {@code search} and {@code value} - and the regular
 * expressions of {@code match} and {@code search} are I-Regexp
 * (<a href="https://www.rfc-editor.org/rfc/rfc9485">RFC 9485</a>), translated into {@link java.util.regex.Pattern}.
 * <p>
 * Two Mercury rules sit beside the standard, and the Rust engine applies the same two:
 * {@link org.platformlambda.mini.jsonpath.JsonPath#compileLenient(java.lang.String)} accepts a hyphen in a
 * dot-notation member name, and {@link org.platformlambda.mini.jsonpath.JsonPath#read(java.lang.Object,
 * java.lang.Object)} shapes the result by the kind of query.
 * <p>
 * The engine depends on {@code java.base} only. Parsing nests at most 64 levels and the descendant segment walks the
 * document with its own stack, so neither a query nor a document can exhaust a thread's stack.
 */
package org.platformlambda.mini.jsonpath;
