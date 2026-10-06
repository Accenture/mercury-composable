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
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * Writes MessagePack values into a growing byte array, each in the smallest format that represents it, as the
 * specification recommends.
 * <p>
 * The integer rule is the one the Java and Rust MessagePack libraries share, so the bytes are identical to theirs: a
 * value from -32 to 127 is one byte (a fixint); a larger non-negative value takes the smallest <em>unsigned</em>
 * width (uint8, uint16, uint32, uint64) and a smaller negative value the smallest <em>signed</em> width (int8, int16,
 * int32, int64). A {@code float} is a float32 and a {@code double} a float64, bit for bit, NaN payloads included. A
 * string is UTF-8 with the shortest str header for its byte length (an unpaired surrogate becomes {@code ?}, as
 * {@code String.getBytes(UTF_8)} writes it); bytes take the shortest bin header. Array and map headers take the
 * shortest form for their count, and the caller then writes the elements, or the keys and values, as values of their
 * own.
 * <p>
 * There is no method for an extension value: this codec writes none.
 * <p>
 * A writer is for one thread; it holds no shared state and takes no lock. {@link #toByteArray()} copies the written
 * bytes, and {@link #reset()} lets the same thread reuse the buffer.
 */
public final class MsgPackWriter {
    private static final VarHandle SHORT_BE = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle INT_BE = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle LONG_BE = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);
    private static final int MAX_SIZE = Integer.MAX_VALUE - 8;
    // a string up to this many chars is first tried as ASCII straight into the buffer, which skips the UTF-8 array
    private static final int ASCII_ATTEMPT = 128;

    private byte[] buffer;
    private int size;

    /**
     * A writer with a 256-byte initial buffer.
     */
    public MsgPackWriter() {
        this(256);
    }

    /**
     * @param initialCapacity the initial buffer size; the buffer grows as needed
     */
    public MsgPackWriter(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initialCapacity must not be negative");
        }
        this.buffer = new byte[initialCapacity];
    }

    /**
     * @return this writer, after writing a nil
     */
    public MsgPackWriter writeNil() {
        ensure(1);
        buffer[size++] = (byte) 0xc0;
        return this;
    }

    /**
     * @param value the boolean to write
     * @return this writer
     */
    public MsgPackWriter writeBoolean(boolean value) {
        ensure(1);
        buffer[size++] = (byte) (value ? 0xc3 : 0xc2);
        return this;
    }

    /**
     * Write an integer in its smallest format.
     *
     * @param value the integer; an int, short or byte widens to it
     * @return this writer
     */
    public MsgPackWriter writeLong(long value) {
        if (value < -(1L << 5)) {
            if (value < -(1L << 15)) {
                if (value < -(1L << 31)) {
                    ensure(9);
                    buffer[size] = (byte) 0xd3;
                    LONG_BE.set(buffer, size + 1, value);
                    size += 9;
                } else {
                    ensure(5);
                    buffer[size] = (byte) 0xd2;
                    INT_BE.set(buffer, size + 1, (int) value);
                    size += 5;
                }
            } else if (value < -(1L << 7)) {
                ensure(3);
                buffer[size] = (byte) 0xd1;
                SHORT_BE.set(buffer, size + 1, (short) value);
                size += 3;
            } else {
                ensure(2);
                buffer[size] = (byte) 0xd0;
                buffer[size + 1] = (byte) value;
                size += 2;
            }
        } else if (value < (1L << 7)) {
            ensure(1);
            buffer[size++] = (byte) value;
        } else if (value < (1L << 8)) {
            ensure(2);
            buffer[size] = (byte) 0xcc;
            buffer[size + 1] = (byte) value;
            size += 2;
        } else if (value < (1L << 16)) {
            ensure(3);
            buffer[size] = (byte) 0xcd;
            SHORT_BE.set(buffer, size + 1, (short) value);
            size += 3;
        } else if (value < (1L << 32)) {
            ensure(5);
            buffer[size] = (byte) 0xce;
            INT_BE.set(buffer, size + 1, (int) value);
            size += 5;
        } else {
            ensure(9);
            buffer[size] = (byte) 0xcf;
            LONG_BE.set(buffer, size + 1, value);
            size += 9;
        }
        return this;
    }

    /**
     * @param value the number to write as a float32, bit for bit
     * @return this writer
     */
    public MsgPackWriter writeFloat(float value) {
        ensure(5);
        buffer[size] = (byte) 0xca;
        INT_BE.set(buffer, size + 1, Float.floatToRawIntBits(value));
        size += 5;
        return this;
    }

    /**
     * @param value the number to write as a float64, bit for bit
     * @return this writer
     */
    public MsgPackWriter writeDouble(double value) {
        ensure(9);
        buffer[size] = (byte) 0xcb;
        LONG_BE.set(buffer, size + 1, Double.doubleToRawLongBits(value));
        size += 9;
        return this;
    }

    /**
     * Write text as a str: UTF-8 bytes under the shortest header for their length.
     *
     * @param text the text
     * @return this writer
     */
    public MsgPackWriter writeString(String text) {
        Objects.requireNonNull(text, "text");
        int chars = text.length();
        if (chars <= ASCII_ATTEMPT) {
            // ASCII text has as many bytes as chars, so the header size is known before the bytes are
            int header = strHeaderSize(chars);
            ensure(header + chars);
            int payload = size + header;
            int i = 0;
            while (i < chars) {
                char c = text.charAt(i);
                if (c >= 0x80) {
                    break;
                }
                buffer[payload + i] = (byte) c;
                i++;
            }
            if (i == chars) {
                writeStrHeader(chars);
                size += chars;
                return this;
            }
        }
        byte[] utf8 = text.getBytes(StandardCharsets.UTF_8);
        writeStrHeader(utf8.length);
        ensure(utf8.length);
        System.arraycopy(utf8, 0, buffer, size, utf8.length);
        size += utf8.length;
        return this;
    }

    /**
     * Write bytes as a bin under the shortest header for their length.
     *
     * @param bytes the bytes
     * @return this writer
     */
    public MsgPackWriter writeBinary(byte[] bytes) {
        return writeBinary(bytes, 0, Objects.requireNonNull(bytes, "bytes").length);
    }

    /**
     * Write a window of a byte array as a bin under the shortest header for its length.
     *
     * @param bytes the array
     * @param offset the first byte to write
     * @param length the number of bytes to write
     * @return this writer
     * @throws IndexOutOfBoundsException if the window does not fit the array
     */
    public MsgPackWriter writeBinary(byte[] bytes, int offset, int length) {
        Objects.requireNonNull(bytes, "bytes");
        Objects.checkFromIndexSize(offset, length, bytes.length);
        if (length < (1 << 8)) {
            ensure(2 + length);
            buffer[size] = (byte) 0xc4;
            buffer[size + 1] = (byte) length;
            size += 2;
        } else if (length < (1 << 16)) {
            ensure(3 + length);
            buffer[size] = (byte) 0xc5;
            SHORT_BE.set(buffer, size + 1, (short) length);
            size += 3;
        } else {
            ensure(5 + length);
            buffer[size] = (byte) 0xc6;
            INT_BE.set(buffer, size + 1, length);
            size += 5;
        }
        System.arraycopy(bytes, offset, buffer, size, length);
        size += length;
        return this;
    }

    /**
     * Write an array header; the caller writes the elements next.
     *
     * @param count the number of elements that follow
     * @return this writer
     * @throws IllegalArgumentException for a negative count
     */
    public MsgPackWriter writeArrayHeader(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("An array cannot have " + count + " elements");
        }
        if (count < (1 << 4)) {
            ensure(1);
            buffer[size++] = (byte) (0x90 | count);
        } else if (count < (1 << 16)) {
            ensure(3);
            buffer[size] = (byte) 0xdc;
            SHORT_BE.set(buffer, size + 1, (short) count);
            size += 3;
        } else {
            ensure(5);
            buffer[size] = (byte) 0xdd;
            INT_BE.set(buffer, size + 1, count);
            size += 5;
        }
        return this;
    }

    /**
     * Write a map header; the caller writes each entry next, the key and then its value.
     *
     * @param count the number of entries that follow
     * @return this writer
     * @throws IllegalArgumentException for a negative count
     */
    public MsgPackWriter writeMapHeader(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("A map cannot have " + count + " entries");
        }
        if (count < (1 << 4)) {
            ensure(1);
            buffer[size++] = (byte) (0x80 | count);
        } else if (count < (1 << 16)) {
            ensure(3);
            buffer[size] = (byte) 0xde;
            SHORT_BE.set(buffer, size + 1, (short) count);
            size += 3;
        } else {
            ensure(5);
            buffer[size] = (byte) 0xdf;
            INT_BE.set(buffer, size + 1, count);
            size += 5;
        }
        return this;
    }

    /**
     * @return the number of bytes written so far
     */
    public int size() {
        return size;
    }

    /**
     * @return a copy of the bytes written so far
     */
    public byte[] toByteArray() {
        return Arrays.copyOf(buffer, size);
    }

    /**
     * Forget the bytes written so far and keep the buffer, for reuse by the same thread.
     */
    public void reset() {
        size = 0;
    }

    private void writeStrHeader(int length) {
        if (length < (1 << 5)) {
            ensure(1);
            buffer[size++] = (byte) (0xa0 | length);
        } else if (length < (1 << 8)) {
            ensure(2);
            buffer[size] = (byte) 0xd9;
            buffer[size + 1] = (byte) length;
            size += 2;
        } else if (length < (1 << 16)) {
            ensure(3);
            buffer[size] = (byte) 0xda;
            SHORT_BE.set(buffer, size + 1, (short) length);
            size += 3;
        } else {
            ensure(5);
            buffer[size] = (byte) 0xdb;
            INT_BE.set(buffer, size + 1, length);
            size += 5;
        }
    }

    private static int strHeaderSize(int length) {
        if (length < (1 << 5)) {
            return 1;
        }
        if (length < (1 << 8)) {
            return 2;
        }
        return length < (1 << 16) ? 3 : 5;
    }

    private void ensure(int more) {
        if (buffer.length - size < more) {
            grow(more);
        }
    }

    private void grow(int more) {
        long needed = (long) size + more;
        if (needed > MAX_SIZE) {
            throw new IllegalStateException("The MessagePack output would exceed " + MAX_SIZE + " bytes");
        }
        // at least what is needed, usually double the buffer, never beyond the maximum (needed <= MAX_SIZE here)
        long doubled = (long) buffer.length * 2 + 16;
        buffer = Arrays.copyOf(buffer, Math.clamp(doubled, (int) needed, MAX_SIZE));
    }
}
