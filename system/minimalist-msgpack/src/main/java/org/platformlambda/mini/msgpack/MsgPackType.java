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

package org.platformlambda.mini.msgpack;

/**
 * The MessagePack type system: the type a value has once its format is decoded.
 * <p>
 * The specification's deserialization table maps the format families onto these types: every integer format is an
 * {@link #INTEGER}, both float widths are a {@link #FLOAT}, the str formats are a {@link #STRING}, the bin formats a
 * {@link #BINARY}, and the fixext and ext formats an {@link #EXTENSION}. This codec reads no extension value: it only
 * recognizes the family so that a reader can skip one safely.
 */
public enum MsgPackType {
    NIL, BOOLEAN, INTEGER, FLOAT, STRING, BINARY, ARRAY, MAP, EXTENSION
}
