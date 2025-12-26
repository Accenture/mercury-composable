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

package org.platformlambda.core;

import org.junit.jupiter.api.Test;
import org.platformlambda.common.TestBase;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.EventEmitter;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpsTest extends TestBase {

    @Test
    void testTlsHandshake() throws Exception{
        TrustManager[] trustAll = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { /* no-op */ }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { /* no-op */ }
                }
        };
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAll, new java.security.SecureRandom());
        SSLSocketFactory factory = sslContext.getSocketFactory();
        try (SSLSocket socket = (SSLSocket) factory.createSocket("localhost", 8443)) {
            socket.startHandshake();
            assertTrue(socket.isConnected(), "SSL handshake should complete and be connected");
        }
    }

    @Test
    void testHttpsRoundTrip() throws ExecutionException, InterruptedException {
        var po = EventEmitter.getInstance();
        var request = new AsyncHttpRequest().setMethod("GET")
                        .setTargetHost("https://127.0.0.1:8443").setUrl("/").setSecure(true).setTrustAllCert(true);
        var event = new EventEnvelope().setTo("async.http.request").setBody(request.toMap());
        var response = po.request(event, 5000).get();
        assertEquals("Hello from HTTPS server", response.getBody());
    }
}
