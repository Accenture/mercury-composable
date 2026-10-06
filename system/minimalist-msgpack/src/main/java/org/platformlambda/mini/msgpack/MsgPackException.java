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

import java.io.IOException;

/**
 * A decoding error: the bytes end before the value does, a byte is not a valid format, a declared length or count
 * exceeds the bytes that follow, an integer does not fit the requested width, or the next value is not of the
 * requested type. The message names the offset.
 * <p>
 * It is a checked {@link IOException}, so a caller that decodes bytes from outside the process handles every failure
 * on one path; the codec throws no unchecked exception for malformed input.
 */
public class MsgPackException extends IOException {

    /**
     * @param message what was found, and at which offset
     */
    public MsgPackException(String message) {
        super(message);
    }
}
