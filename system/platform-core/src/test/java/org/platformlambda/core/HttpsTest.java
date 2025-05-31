package org.platformlambda.core;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.junit.jupiter.api.Test;
import org.platformlambda.common.TestBase;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HttpsTest extends TestBase {

    @Test
    void testTlsHandshake() throws Exception{
        TrustManager[] trustAll = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
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
    void testHttpsRoundTrip() throws Exception {
        final BlockingQueue<String> bench = new ArrayBlockingQueue<>(1);
        WebClientOptions options = new WebClientOptions().setSsl(true).setTrustAll(true).setVerifyHost(false);
        WebClient client = WebClient.create(Vertx.vertx(), options);
        client.get(8443, "localhost", "/")
                .send(event -> {
                    if (event.succeeded()) {
                        String body = event.result().bodyAsString();
                        bench.add(body);
                    } else {
                        throw new RuntimeException(event.cause());
                    }
                });


        String response = bench.poll(20, TimeUnit.SECONDS);
        assertEquals("Hello from HTTPS server", response);
    }
}
