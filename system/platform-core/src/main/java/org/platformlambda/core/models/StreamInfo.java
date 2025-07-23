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

package org.platformlambda.core.models;

/**
 * This is reserved for system use.
 * DO NOT use this directly in your application code.
 */
public class StreamInfo {
    private final long created;
    private final long expiryMills;
    private long updated;

    public StreamInfo(long expirySeconds) {
        this.created = System.currentTimeMillis();
        this.setUpdated(this.created);
        this.expiryMills = expirySeconds * 1000;
    }

    public long getCreated() {
        return created;
    }

    public long getExpiryMills() {
        return expiryMills;
    }

    public long getUpdated() {
        return updated;
    }

    public void setUpdated(long updated) {
        this.updated = updated;
    }
}
