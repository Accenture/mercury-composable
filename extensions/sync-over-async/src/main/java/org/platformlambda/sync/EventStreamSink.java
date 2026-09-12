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

package org.platformlambda.sync;

import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.system.EventStreamWriter;

import java.util.function.Consumer;

/**
 * The canonical {@code beginStream} sink: forwards each drained {@link StreamSegment} into an
 * {@link EventStreamWriter} bound to the HTTP request, so the shipped {@code x-event-stream} edge does
 * the last mile (SSE/chunked framing, ordered reply lane, back-pressure, disconnect handling):
 * <ul>
 *   <li>{@code data} → {@code write} (named when the segment carries an SSE event name);</li>
 *   <li>{@code eof} → {@code close} (the segment body rides as the terminal event's metadata);</li>
 *   <li>{@code exception} → {@code fail} with HTTP 500 and the segment body as the message.</li>
 * </ul>
 * Most facades want {@link StreamBridge}, which combines this sink with the idle-expiry lifecycle;
 * this class stands alone for facades that manage their own lifecycle.
 */
public class EventStreamSink implements Consumer<StreamSegment> {

    private final EventStreamWriter writer;

    public EventStreamSink(EventStreamWriter writer) {
        this.writer = writer;
    }

    @Override
    public void accept(StreamSegment segment) {
        switch (segment.type()) {
            case StreamSegment.EOF -> writer.close(segment.body());
            case StreamSegment.EXCEPTION -> writer.fail(new AppException(500,
                    segment.body() == null ? "Stream failed" : segment.body()));
            default -> {
                String body = segment.body() == null ? "" : segment.body();
                if (segment.name() == null) {
                    writer.write(body);
                } else {
                    writer.write(segment.name(), body);
                }
            }
        }
    }
}
