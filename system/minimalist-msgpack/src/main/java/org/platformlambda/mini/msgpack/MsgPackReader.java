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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * Reads MessagePack values from a byte array, one value at a time, in the order the bytes hold them.
 * <p>
 * The reader is a cursor. {@link #nextFormat()} looks at the next value's format without consuming it, so a caller
 * dispatches on {@link MsgPackFormat#getType()} and then calls the read method for that type; a container's header
 * method returns its element count and the elements follow as values of their own. The reader does not recurse: a
 * caller that builds nested structures recurses itself and bounds its own depth.
 * <p>
 * Every read method either consumes exactly one value (or one header) or throws a {@link MsgPackException} and leaves
 * the position where it was, so a caller may recover - for example by calling {@link #readBigInteger()} after
 * {@link #readLong()} refused a uint64 above {@code Long.MAX_VALUE}. The checks a hostile byte array meets:
 * <ul>
 * <li>every read is bounded by the array window; bytes that end before the value does are refused by offset;</li>
 * <li>the lengths of str, bin and ext values and the counts of arrays and maps are 32-bit <em>unsigned</em> and are
 *     checked against the bytes that remain before anything is allocated or consumed - an array of N elements needs
 *     at least N bytes and a map of N entries at least 2N, so a header that promises more than the input holds is
 *     refused at the header;</li>
 * <li>{@code 0xc1}, which the specification never uses, is refused as a format byte;</li>
 * <li>{@link #skipValue()} is iterative, so skipping a deeply nested value costs no stack;</li>
 * <li>no exception other than {@link MsgPackException} (and {@link NullPointerException} or
 *     {@link IndexOutOfBoundsException} for a null array or an invalid window at construction) leaves the reader.</li>
 * </ul>
 * As the specification allows, a str value may hold bytes that are not valid UTF-8; {@link #readString()} decodes them
 * leniently, substituting U+FFFD, exactly as {@code new String(bytes, UTF_8)} does. The str and bin families are
 * accepted interchangeably by {@link #readString()} and {@link #readBinary()}, because older encoders wrote text with
 * the raw (bin-less) format; the format reported by {@link #nextFormat()} tells them apart when that matters.
 * <p>
 * Extension values are not decoded: {@link #skipValue()} steps over one, and there is no method to read its payload.
 * <p>
 * A reader is for one thread; it holds no shared state and takes no lock.
 */
public final class MsgPackReader {
    private static final VarHandle SHORT_BE = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle INT_BE = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle LONG_BE = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);

    private final byte[] buffer;
    private final int limit;
    private int position;

    /**
     * A reader over a whole byte array.
     *
     * @param bytes the MessagePack bytes
     */
    public MsgPackReader(byte[] bytes) {
        this(bytes, 0, Objects.requireNonNull(bytes, "bytes").length);
    }

    /**
     * A reader over a window of a byte array. Bytes outside the window are never read.
     *
     * @param bytes the array
     * @param offset the first byte of the window
     * @param length the number of bytes in the window
     * @throws IndexOutOfBoundsException if the window does not fit the array
     */
    public MsgPackReader(byte[] bytes, int offset, int length) {
        Objects.requireNonNull(bytes, "bytes");
        Objects.checkFromIndexSize(offset, length, bytes.length);
        this.buffer = bytes;
        this.position = offset;
        this.limit = offset + length;
    }

    /**
     * @return true when at least one byte remains, that is, when another value may follow
     */
    public boolean hasNext() {
        return position < limit;
    }

    /**
     * @return the offset of the next byte to read, in the underlying array
     */
    public int position() {
        return position;
    }

    /**
     * @return the number of bytes not yet read
     */
    public int remaining() {
        return limit - position;
    }

    /**
     * The format of the next value, without consuming anything.
     *
     * @return the format
     * @throws MsgPackException when no byte remains or the byte is {@code 0xc1}, which the specification never uses
     */
    public MsgPackFormat nextFormat() throws MsgPackException {
        if (position >= limit) {
            throw new MsgPackException("Unexpected end of input at offset " + position);
        }
        MsgPackFormat format = MsgPackFormat.of(buffer[position]);
        if (format == null) {
            throw new MsgPackException("Invalid format byte 0xc1 at offset " + position);
        }
        return format;
    }

    /**
     * Consume a nil.
     *
     * @throws MsgPackException when the next value is not nil
     */
    public void readNil() throws MsgPackException {
        MsgPackFormat format = nextFormat();
        if (format != MsgPackFormat.NIL) {
            throw typeError("nil", format);
        }
        position++;
    }

    /**
     * Consume a boolean.
     *
     * @return its value
     * @throws MsgPackException when the next value is not a boolean
     */
    public boolean readBoolean() throws MsgPackException {
        MsgPackFormat format = nextFormat();
        if (format != MsgPackFormat.BOOLEAN) {
            throw typeError("a boolean", format);
        }
        return buffer[position++] == (byte) 0xc3;
    }

    /**
     * Consume an integer of any format and return it as a long.
     *
     * @return the value
     * @throws MsgPackException when the next value is not an integer, or is a uint64 above {@code Long.MAX_VALUE}
     *         (the position is unchanged, so {@link #readBigInteger()} can read it)
     */
    public long readLong() throws MsgPackException {
        MsgPackFormat format = nextFormat();
        int start = position;
        long value;
        switch (format) {
            case POSITIVE_FIXINT, NEGATIVE_FIXINT -> {
                value = buffer[start];
                position++;
            }
            case UINT8 -> {
                require(2);
                value = buffer[start + 1] & 0xff;
                position += 2;
            }
            case UINT16 -> {
                require(3);
                value = (short) SHORT_BE.get(buffer, start + 1) & 0xffff;
                position += 3;
            }
            case UINT32 -> {
                require(5);
                value = (int) INT_BE.get(buffer, start + 1) & 0xffffffffL;
                position += 5;
            }
            case UINT64 -> {
                require(9);
                value = (long) LONG_BE.get(buffer, start + 1);
                if (value < 0) {
                    throw new MsgPackException("Integer overflow: uint64 " + Long.toUnsignedString(value) +
                            " does not fit a long at offset " + start);
                }
                position += 9;
            }
            case INT8 -> {
                require(2);
                value = buffer[start + 1];
                position += 2;
            }
            case INT16 -> {
                require(3);
                value = (short) SHORT_BE.get(buffer, start + 1);
                position += 3;
            }
            case INT32 -> {
                require(5);
                value = (int) INT_BE.get(buffer, start + 1);
                position += 5;
            }
            case INT64 -> {
                require(9);
                value = (long) LONG_BE.get(buffer, start + 1);
                position += 9;
            }
            default -> throw typeError("an integer", format);
        }
        return value;
    }

    /**
     * Consume an integer of any format, a uint64 of any magnitude included.
     *
     * @return the value
     * @throws MsgPackException when the next value is not an integer
     */
    public BigInteger readBigInteger() throws MsgPackException {
        MsgPackFormat format = nextFormat();
        if (format == MsgPackFormat.UINT64) {
            require(9);
            long value = (long) LONG_BE.get(buffer, position + 1);
            position += 9;
            return value < 0 ? new BigInteger(Long.toUnsignedString(value)) : BigInteger.valueOf(value);
        }
        return BigInteger.valueOf(readLong());
    }

    /**
     * Consume a float32.
     *
     * @return the value
     * @throws MsgPackException when the next value is not a float32 (a float64 is not narrowed - use
     *         {@link #readDouble()}, which accepts both widths)
     */
    public float readFloat() throws MsgPackException {
        MsgPackFormat format = nextFormat();
        if (format != MsgPackFormat.FLOAT32) {
            throw typeError("a float32", format);
        }
        require(5);
        float value = Float.intBitsToFloat((int) INT_BE.get(buffer, position + 1));
        position += 5;
        return value;
    }

    /**
     * Consume a float32 or a float64 and return it as a double; a float32 widens without loss.
     *
     * @return the value
     * @throws MsgPackException when the next value is not a float
     */
    public double readDouble() throws MsgPackException {
        MsgPackFormat format = nextFormat();
        double value;
        switch (format) {
            case FLOAT32 -> {
                require(5);
                value = Float.intBitsToFloat((int) INT_BE.get(buffer, position + 1));
                position += 5;
            }
            case FLOAT64 -> {
                require(9);
                value = Double.longBitsToDouble((long) LONG_BE.get(buffer, position + 1));
                position += 9;
            }
            default -> throw typeError("a float", format);
        }
        return value;
    }

    /**
     * Consume a str (or a bin, read as UTF-8 text) of any format.
     *
     * @return the text, decoded leniently (an invalid byte sequence becomes U+FFFD)
     * @throws MsgPackException when the next value is not a str or bin, or its bytes end before its declared length
     */
    public String readString() throws MsgPackException {
        int length = readRawLength("a str");
        String text = new String(buffer, position, length, StandardCharsets.UTF_8);
        position += length;
        return text;
    }

    /**
     * Consume a bin (or a str, read as raw bytes) of any format.
     *
     * @return a copy of the bytes
     * @throws MsgPackException when the next value is not a bin or str, or its bytes end before its declared length
     */
    public byte[] readBinary() throws MsgPackException {
        int length = readRawLength("a bin");
        byte[] bytes = Arrays.copyOfRange(buffer, position, position + length);
        position += length;
        return bytes;
    }

    /**
     * Consume an array header. The elements follow as values of their own.
     *
     * @return the number of elements
     * @throws MsgPackException when the next value is not an array, or the header declares more elements than the
     *         remaining bytes can hold (one byte per element at the least)
     */
    public int readArrayHeader() throws MsgPackException {
        MsgPackFormat format = nextFormat();
        int start = position;
        long count;
        switch (format) {
            case FIXARRAY -> {
                count = buffer[start] & 0x0f;
                position++;
            }
            case ARRAY16 -> {
                require(3);
                count = (short) SHORT_BE.get(buffer, start + 1) & 0xffff;
                position += 3;
            }
            case ARRAY32 -> {
                require(5);
                count = (int) INT_BE.get(buffer, start + 1) & 0xffffffffL;
                position += 5;
            }
            default -> throw typeError("an array", format);
        }
        return checkedCount(format, count, 1, start);
    }

    /**
     * Consume a map header. The entries follow as key and value pairs, each a value of its own.
     *
     * @return the number of entries
     * @throws MsgPackException when the next value is not a map, or the header declares more entries than the
     *         remaining bytes can hold (two bytes per entry at the least)
     */
    public int readMapHeader() throws MsgPackException {
        MsgPackFormat format = nextFormat();
        int start = position;
        long count;
        switch (format) {
            case FIXMAP -> {
                count = buffer[start] & 0x0f;
                position++;
            }
            case MAP16 -> {
                require(3);
                count = (short) SHORT_BE.get(buffer, start + 1) & 0xffff;
                position += 3;
            }
            case MAP32 -> {
                require(5);
                count = (int) INT_BE.get(buffer, start + 1) & 0xffffffffL;
                position += 5;
            }
            default -> throw typeError("a map", format);
        }
        return checkedCount(format, count, 2, start);
    }

    /**
     * Consume the next value whatever it is, an extension value or a nested container included, without decoding it.
     * The walk is iterative, so its cost in stack does not depend on the nesting.
     *
     * @throws MsgPackException when the bytes are not a complete, well-formed value (the position is unchanged)
     */
    public void skipValue() throws MsgPackException {
        int start = position;
        try {
            long pending = 1;
            while (pending > 0) {
                MsgPackFormat format = nextFormat();
                pending--;
                switch (format) {
                    case POSITIVE_FIXINT, NEGATIVE_FIXINT, NIL, BOOLEAN -> position++;
                    case UINT8, INT8 -> advance(2);
                    case UINT16, INT16 -> advance(3);
                    case UINT32, INT32, FLOAT32 -> advance(5);
                    case UINT64, INT64, FLOAT64 -> advance(9);
                    case FIXEXT1 -> advance(3);
                    case FIXEXT2 -> advance(4);
                    case FIXEXT4 -> advance(6);
                    case FIXEXT8 -> advance(10);
                    case FIXEXT16 -> advance(18);
                    case FIXSTR, STR8, STR16, STR32, BIN8, BIN16, BIN32 -> {
                        int length = readRawLength("a str or bin");
                        position += length;
                    }
                    case EXT8, EXT16, EXT32 -> {
                        int length = readExtensionLength();
                        position += length;
                    }
                    case FIXARRAY, ARRAY16, ARRAY32 -> pending += readArrayHeader();
                    case FIXMAP, MAP16, MAP32 -> pending += 2L * readMapHeader();
                }
            }
        } catch (MsgPackException e) {
            position = start;
            throw e;
        }
    }

    /**
     * Consume a str or bin header and return the payload length, checked against the remaining bytes.
     */
    private int readRawLength(String expected) throws MsgPackException {
        MsgPackFormat format = nextFormat();
        int start = position;
        long length;
        switch (format) {
            case FIXSTR -> {
                length = buffer[start] & 0x1f;
                position++;
            }
            case STR8, BIN8 -> {
                require(2);
                length = buffer[start + 1] & 0xff;
                position += 2;
            }
            case STR16, BIN16 -> {
                require(3);
                length = (short) SHORT_BE.get(buffer, start + 1) & 0xffff;
                position += 3;
            }
            case STR32, BIN32 -> {
                require(5);
                length = (int) INT_BE.get(buffer, start + 1) & 0xffffffffL;
                position += 5;
            }
            default -> throw typeError(expected, format);
        }
        return checkedLength(format, length, start);
    }

    /**
     * Consume an ext8, ext16 or ext32 header (format, length and type bytes) and return the payload length, checked
     * against the remaining bytes.
     */
    private int readExtensionLength() throws MsgPackException {
        MsgPackFormat format = nextFormat();
        int start = position;
        long length;
        switch (format) {
            case EXT8 -> {
                require(3);
                length = buffer[start + 1] & 0xff;
                position += 3;
            }
            case EXT16 -> {
                require(4);
                length = (short) SHORT_BE.get(buffer, start + 1) & 0xffff;
                position += 4;
            }
            case EXT32 -> {
                require(6);
                length = (int) INT_BE.get(buffer, start + 1) & 0xffffffffL;
                position += 6;
            }
            default -> throw typeError("an ext", format);
        }
        return checkedLength(format, length, start);
    }

    private int checkedLength(MsgPackFormat format, long length, int start) throws MsgPackException {
        int available = limit - position;
        if (length > available) {
            position = start;
            throw new MsgPackException(format + " declares " + length + " byte(s) but only " + available +
                    " follow at offset " + start);
        }
        return (int) length;
    }

    private int checkedCount(MsgPackFormat format, long count, int bytesPerElement, int start)
            throws MsgPackException {
        int available = limit - position;
        if (count * bytesPerElement > available) {
            position = start;
            throw new MsgPackException(format + " declares " + count + " element(s) but only " + available +
                    " byte(s) follow at offset " + start);
        }
        return (int) count;
    }

    private void require(int n) throws MsgPackException {
        if (limit - position < n) {
            throw new MsgPackException("Unexpected end of input: " + n + " byte(s) needed at offset " + position +
                    " but " + (limit - position) + " remain");
        }
    }

    private void advance(int n) throws MsgPackException {
        require(n);
        position += n;
    }

    private MsgPackException typeError(String expected, MsgPackFormat found) {
        return new MsgPackException("Expected " + expected + " but found " + found + " at offset " + position);
    }
}
