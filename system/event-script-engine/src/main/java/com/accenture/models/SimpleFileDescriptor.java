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

package com.accenture.models;

public class SimpleFileDescriptor {
    private static final String CLOSE_BRACKET = ")";
    private static final String FILE_TYPE = "file(";
    private static final String CLASSPATH_TYPE = "classpath(";
    private static final String TEXT_FILE = "text:";
    private static final String JSON_FILE = "json:";
    private static final String BINARY_FILE = "binary:";
    private static final String APPEND_MODE = "append:";
    public final String fileName;
    public enum FILE_MODE {
        TEXT,
        BINARY,
        JSON,
        APPEND
    }
    public final FILE_MODE mode;

    public SimpleFileDescriptor(String value) {
        int last = value.lastIndexOf(CLOSE_BRACKET);
        int offset = 0;
        if (value.startsWith(FILE_TYPE)) {
            offset = FILE_TYPE.length();
        } else if (value.startsWith(CLASSPATH_TYPE)) {
            offset = CLASSPATH_TYPE.length();
        }
        final String name;
        final String filePath = value.substring(offset, last).trim();
        if (filePath.startsWith(TEXT_FILE)) {
            name = filePath.substring(TEXT_FILE.length());
            mode = FILE_MODE.TEXT;
        } else if (filePath.startsWith(JSON_FILE)) {
            name = filePath.substring(JSON_FILE.length());
            mode = FILE_MODE.JSON;
        } else if (filePath.startsWith(BINARY_FILE)) {
            name = filePath.substring(BINARY_FILE.length());
            mode = FILE_MODE.BINARY;
        } else if (filePath.startsWith(APPEND_MODE)) {
            name = filePath.substring(APPEND_MODE.length());
            mode = FILE_MODE.APPEND;
        } else {
            name = filePath;
            mode = FILE_MODE.BINARY;
        }
        fileName = name.startsWith("/")? name : "/".concat(name);
    }
}
