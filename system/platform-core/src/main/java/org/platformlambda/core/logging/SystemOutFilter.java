/*

    Copyright 2018-2025 Accenture Technology

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

package org.platformlambda.core.logging;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * This is reserved for system use.
 * DO NOT use this directly in your application code.
 */
public class SystemOutFilter extends PrintStream {

    public SystemOutFilter(OutputStream out) {
        super(out);
    }

    @Override
    public void print(String text) {
        // only accept text with JSON signature from JsonAppender and CompactAppender
        if (text != null && text.startsWith("{") && text.endsWith("}\n")) {
            super.print(text);
        }
    }

    @Override
    public void println(String text) {
        // no-op to disable other type of System.out
    }

    @Override
    public void println() {
        // no-op to disable other type of System.out
    }

    @Override
    public void println(boolean b) {
        // no-op to disable other type of System.out
    }

    @Override
    public void println(char c) {
        // no-op to disable other type of System.out
    }

    @Override
    public void println(int i) {
        // no-op to disable other type of System.out
    }

    @Override
    public void println(long l) {
        // no-op to disable other type of System.out
    }

    @Override
    public void println(float f) {
        // no-op to disable other type of System.out
    }

    @Override
    public void println(double d) {
        // no-op to disable other type of System.out
    }

    @Override
    public void println(char s[]) {
        // no-op to disable other type of System.out
    }

    @Override
    public void println(Object o) {
        // no-op to disable other type of System.out
    }

    @Override
    public void print(boolean b) {
        // no-op to disable other type of System.out
    }

    @Override
    public void print(char c) {
        // no-op to disable other type of System.out
    }

    @Override
    public void print(int i) {
        // no-op to disable other type of System.out
    }

    @Override
    public void print(long l) {
        // no-op to disable other type of System.out
    }

    @Override
    public void print(float f) {
        // no-op to disable other type of System.out
    }

    @Override
    public void print(double d) {
        // no-op to disable other type of System.out
    }

    @Override
    public void print(char s[]) {
        // no-op to disable other type of System.out
    }

    @Override
    public void print(Object o) {
        // no-op to disable other type of System.out
    }
}
