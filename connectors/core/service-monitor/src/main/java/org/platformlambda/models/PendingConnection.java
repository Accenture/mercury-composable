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

package org.platformlambda.models;

public class PendingConnection {

    public enum PendingType {
        CONNECTED, HANDSHAKE
    }

    public final String origin;
    public final String session;
    public final long created = System.currentTimeMillis();
    public PendingType type;

    public PendingConnection(String origin, String session) {
        this.origin = origin;
        this.session = session;
        this.type = PendingType.CONNECTED;
    }

    public PendingConnection setType(PendingType type) {
        this.type = type;
        return this;
    }
}
