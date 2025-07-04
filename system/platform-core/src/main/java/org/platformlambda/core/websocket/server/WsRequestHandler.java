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

package org.platformlambda.core.websocket.server;

import io.vertx.core.Handler;
import io.vertx.core.http.ServerWebSocket;
import org.platformlambda.core.annotations.EventInterceptor;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.Kv;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.CryptoApi;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is reserved for system use.
 * DO NOT use this directly in your application code.
 * <p>
 * Websocket request handler
 */
public class WsRequestHandler implements Handler<ServerWebSocket> {
    private static final Logger log = LoggerFactory.getLogger(WsRequestHandler.class);
    private static final CryptoApi crypto = new CryptoApi();
    private static final String HOUSEKEEPER = "system.ws.server.cleanup";
    private static final String IN = ".in";
    private static final String OUT = ".out";
    private static final long HOUSEKEEPING_INTERVAL = 10 * 1000L;      // 10 seconds
    private static final ConcurrentMap<String, WsEnvelope> connections = new ConcurrentHashMap<>();
    private static final AtomicInteger counter = new AtomicInteger(0);
    private final ConcurrentMap<String, LambdaFunction> lambdas;
    private final List<String> wsPaths;

    public WsRequestHandler(ConcurrentMap<String, LambdaFunction> lambdas, List<String> wsPaths) {
        this.lambdas = lambdas;
        this.wsPaths = new ArrayList<>(wsPaths);
        Platform platform = Platform.getInstance();
        IdleCheck idle = new IdleCheck();
        platform.getVertx().setPeriodic(HOUSEKEEPING_INTERVAL, t -> idle.removeExpiredConnections());
        log.info("Housekeeper started");
        platform.registerPrivate(HOUSEKEEPER, new WsHousekeeper(), 1);
    }

    @Override
    public void handle(ServerWebSocket ws) {
        String uri = ws.path().trim();
        String path = findPath(uri);
        if (path != null) {
            final Platform platform = Platform.getInstance();
            final EventEmitter po = EventEmitter.getInstance();
            final String ip = ws.remoteAddress().hostAddress();
            final String token = uri.substring(uri.lastIndexOf('/')+1);
            final String query = ws.query();
            // generate a 6-digit random number as identifier
            final int r = crypto.nextInt(100000, 1000000);
            // ensure uniqueness using a monotonically increasing sequence number
            final int n = counter.incrementAndGet();
            final String session = "ws."+r+"."+n;
            final String rxPath = session+IN;
            final String txPath = session+OUT;
            WsEnvelope md = new WsEnvelope(ws, path, rxPath, txPath);
            connections.put(session, md);
            log.info("Session {} connected", session);
            platform.registerPrivate(rxPath, lambdas.get(path), 1);
            platform.registerPrivate(txPath, new WsServerTransmitter(ws), 1);
            try {
                po.send(rxPath, new Kv(WsEnvelope.TYPE, WsEnvelope.OPEN),
                        new Kv(WsEnvelope.ROUTE, rxPath), new Kv(WsEnvelope.TX_PATH, txPath),
                        new Kv(WsEnvelope.IP, ip), new Kv(WsEnvelope.PATH, path),
                        new Kv(WsEnvelope.QUERY, query),
                        new Kv(WsEnvelope.TOKEN, token));
            } catch (IllegalArgumentException e) {
                log.error("Unable to send open signal to {} - {}", rxPath, e.getMessage());
            }
            ws.binaryMessageHandler(b -> {
                md.touch();
                try {
                    po.send(rxPath, b.getBytes(), new Kv(WsEnvelope.TYPE, WsEnvelope.BYTES),
                            new Kv(WsEnvelope.ROUTE, rxPath), new Kv(WsEnvelope.TX_PATH, txPath));
                } catch (IllegalArgumentException e) {
                    log.warn("Unable to send binary message to {} - {}", rxPath, e.getMessage());
                }
            });
            ws.textMessageHandler(text -> {
                md.touch();
                try {
                    po.send(rxPath, text, new Kv(WsEnvelope.TYPE, WsEnvelope.STRING),
                            new Kv(WsEnvelope.ROUTE, rxPath), new Kv(WsEnvelope.TX_PATH, txPath));
                } catch (IllegalArgumentException e) {
                    log.warn("Unable to send text message to {} - {}", rxPath, e.getMessage());
                }
            });
            ws.closeHandler(close -> {
                String reason = ws.closeReason() == null? "ok" : ws.closeReason();
                log.info("Session {} closed ({}, {})", session, ws.closeStatusCode(), reason);
                connections.remove(session);
                // send the close signal to the websocket listener function and then tell housekeeper to clean up
                EventEnvelope closeSignal = new EventEnvelope().setTo(rxPath);
                closeSignal.setHeader(WsEnvelope.ROUTE, rxPath)
                            .setHeader(WsEnvelope.TOKEN, token)
                            .setHeader(WsEnvelope.CLOSE_CODE, ws.closeStatusCode())
                            .setHeader(WsEnvelope.CLOSE_REASON, reason)
                            .setHeader(WsEnvelope.TYPE, WsEnvelope.CLOSE)
                            .setReplyTo(HOUSEKEEPER).setCorrelationId(session);
                try {
                    po.send(closeSignal);
                } catch (IllegalArgumentException e) {
                    platform.release(rxPath);
                    platform.release(txPath);
                    log.error("Unable to send close signal to {} - {}", rxPath, e.getMessage());
                }
            });
            ws.exceptionHandler(e -> {
                log.warn("Session {} exception - {}", session, e.getMessage());
                if (!ws.isClosed()) {
                    ws.close();
                }
            });
        }
    }

    private String findPath(String path) {
        for (String u: wsPaths) {
            String prefix = u + "/";
            if (path.startsWith(prefix) && !path.equals(prefix)) {
                return u;
            }
        }
        return null;
    }

    private static class IdleCheck {
        private final int timeout;
        private final long expiry;

        private IdleCheck() {
            final AppConfigReader config = AppConfigReader.getInstance();
            final Utility util = Utility.getInstance();
            timeout = Math.min(30, util.str2int(config.getProperty("websocket.idle.timeout", "60")));
            expiry = timeout * 1000L;
            String timer = util.elapsedTime(expiry);
            log.info("Websocket server idle expiry {}", timer);
        }

        private void removeExpiredConnections() {
            long now = System.currentTimeMillis();
            List<String> sessions = new ArrayList<>();
            for (Map.Entry<String, WsEnvelope> kv : connections.entrySet()) {
                WsEnvelope md = kv.getValue();
                if (now - md.getLastAccess() > expiry) {
                    sessions.add(kv.getKey());
                }
            }
            for (String conn : sessions) {
                WsEnvelope md = connections.get(conn);
                if (md != null) {
                    connections.remove(conn);
                    log.warn("Websocket {} expired ({}, {})", md.getUriPath(), md.getRxPath(), md.getTxPath());
                    // drop connection due to inactivity
                    if (!md.getWebSocket().isClosed()) {
                        md.getWebSocket().close((short) 1003, "Idle for " + timeout + " seconds");
                    }
                }
            }
        }
    }

    @EventInterceptor
    private static class WsHousekeeper implements LambdaFunction {

        @Override
        public Object handleEvent(Map<String, String> headers, Object input, int instance) {
            if (input instanceof EventEnvelope event) {
                String session = event.getCorrelationId();
                if (session != null) {
                    Platform platform = Platform.getInstance();
                    String rxPath = session + IN;
                    String txPath = session + OUT;
                    platform.release(rxPath);
                    platform.release(txPath);
                }
            }
            return null;
        }
    }
}
