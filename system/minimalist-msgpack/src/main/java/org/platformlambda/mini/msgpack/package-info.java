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
 * A minimalist MessagePack codec written from the published specification
 * (<a href="https://github.com/msgpack/msgpack/blob/master/spec.md">spec.md</a>).
 * <p>
 * {@link org.platformlambda.mini.msgpack.MsgPackWriter} writes and {@link org.platformlambda.mini.msgpack.MsgPackReader}
 * reads the nil, bool, int, float, str, bin, array and map families, one value at a time. Extension types are not
 * supported: the writer has no method for them and the reader decodes none, but it knows their framing so that
 * {@link org.platformlambda.mini.msgpack.MsgPackReader#skipValue()} steps over one without losing its place.
 * <p>
 * The codec depends on {@code java.base} only. Multi-byte fields are read and written through
 * {@link java.lang.invoke.VarHandle} views of the byte array (big-endian, as the specification requires), never through
 * {@code sun.misc.Unsafe}. A reader or writer holds no shared or thread-local state and takes no lock, so it is safe to
 * create one per call on a virtual thread; one instance is for one thread.
 */
package org.platformlambda.mini.msgpack;
