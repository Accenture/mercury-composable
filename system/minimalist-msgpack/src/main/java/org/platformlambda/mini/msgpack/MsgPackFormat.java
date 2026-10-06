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
 * The MessagePack formats, one per row of the specification's format table, each with the {@link MsgPackType} it
 * decodes to.
 * <p>
 * A format is identified by the first byte of a value. {@link #of(byte)} is that table: the fixed-width families
 * (positive fixint {@code 0x00-0x7f}, fixmap {@code 0x80-0x8f}, fixarray {@code 0x90-0x9f}, fixstr {@code 0xa0-0xbf},
 * negative fixint {@code 0xe0-0xff}) carry their value or length in the first byte itself, {@code 0xc0-0xdf} are the
 * single-byte markers, and {@code 0xc1} is "never used" by the specification, so it has no format and {@link #of(byte)}
 * returns {@code null} for it.
 */
public enum MsgPackFormat {
    POSITIVE_FIXINT(MsgPackType.INTEGER),
    FIXMAP(MsgPackType.MAP),
    FIXARRAY(MsgPackType.ARRAY),
    FIXSTR(MsgPackType.STRING),
    NIL(MsgPackType.NIL),
    BOOLEAN(MsgPackType.BOOLEAN),
    BIN8(MsgPackType.BINARY),
    BIN16(MsgPackType.BINARY),
    BIN32(MsgPackType.BINARY),
    EXT8(MsgPackType.EXTENSION),
    EXT16(MsgPackType.EXTENSION),
    EXT32(MsgPackType.EXTENSION),
    FLOAT32(MsgPackType.FLOAT),
    FLOAT64(MsgPackType.FLOAT),
    UINT8(MsgPackType.INTEGER),
    UINT16(MsgPackType.INTEGER),
    UINT32(MsgPackType.INTEGER),
    UINT64(MsgPackType.INTEGER),
    INT8(MsgPackType.INTEGER),
    INT16(MsgPackType.INTEGER),
    INT32(MsgPackType.INTEGER),
    INT64(MsgPackType.INTEGER),
    FIXEXT1(MsgPackType.EXTENSION),
    FIXEXT2(MsgPackType.EXTENSION),
    FIXEXT4(MsgPackType.EXTENSION),
    FIXEXT8(MsgPackType.EXTENSION),
    FIXEXT16(MsgPackType.EXTENSION),
    STR8(MsgPackType.STRING),
    STR16(MsgPackType.STRING),
    STR32(MsgPackType.STRING),
    ARRAY16(MsgPackType.ARRAY),
    ARRAY32(MsgPackType.ARRAY),
    MAP16(MsgPackType.MAP),
    MAP32(MsgPackType.MAP),
    NEGATIVE_FIXINT(MsgPackType.INTEGER);

    private static final MsgPackFormat[] BY_FIRST_BYTE = table();

    private final MsgPackType type;

    MsgPackFormat(MsgPackType type) {
        this.type = type;
    }

    /**
     * @return the type a value of this format decodes to
     */
    public MsgPackType getType() {
        return type;
    }

    /**
     * The format table of the specification.
     *
     * @param firstByte the first byte of a value
     * @return its format, or null for {@code 0xc1}, which the specification never uses
     */
    public static MsgPackFormat of(byte firstByte) {
        return BY_FIRST_BYTE[firstByte & 0xff];
    }

    private static MsgPackFormat[] table() {
        var t = new MsgPackFormat[256];
        for (int b = 0x00; b <= 0x7f; b++) {
            t[b] = POSITIVE_FIXINT;
        }
        for (int b = 0x80; b <= 0x8f; b++) {
            t[b] = FIXMAP;
        }
        for (int b = 0x90; b <= 0x9f; b++) {
            t[b] = FIXARRAY;
        }
        for (int b = 0xa0; b <= 0xbf; b++) {
            t[b] = FIXSTR;
        }
        t[0xc0] = NIL;
        // 0xc1 is never used by the specification and stays null
        t[0xc2] = BOOLEAN;
        t[0xc3] = BOOLEAN;
        t[0xc4] = BIN8;
        t[0xc5] = BIN16;
        t[0xc6] = BIN32;
        t[0xc7] = EXT8;
        t[0xc8] = EXT16;
        t[0xc9] = EXT32;
        t[0xca] = FLOAT32;
        t[0xcb] = FLOAT64;
        t[0xcc] = UINT8;
        t[0xcd] = UINT16;
        t[0xce] = UINT32;
        t[0xcf] = UINT64;
        t[0xd0] = INT8;
        t[0xd1] = INT16;
        t[0xd2] = INT32;
        t[0xd3] = INT64;
        t[0xd4] = FIXEXT1;
        t[0xd5] = FIXEXT2;
        t[0xd6] = FIXEXT4;
        t[0xd7] = FIXEXT8;
        t[0xd8] = FIXEXT16;
        t[0xd9] = STR8;
        t[0xda] = STR16;
        t[0xdb] = STR32;
        t[0xdc] = ARRAY16;
        t[0xdd] = ARRAY32;
        t[0xde] = MAP16;
        t[0xdf] = MAP32;
        for (int b = 0xe0; b <= 0xff; b++) {
            t[b] = NEGATIVE_FIXINT;
        }
        return t;
    }
}
