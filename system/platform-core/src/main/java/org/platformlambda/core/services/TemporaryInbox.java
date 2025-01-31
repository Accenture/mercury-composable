package org.platformlambda.core.services;

import org.platformlambda.core.annotations.EventInterceptor;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.InboxBase;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.Platform;

import java.util.Map;

@EventInterceptor
@PreLoad(route= TemporaryInbox.ROUTE, instances = 100)
public class TemporaryInbox implements TypedLambdaFunction<EventEnvelope, Void> {
    public static final String ROUTE = "temporary.inbox";

    @Override
    public Void handleEvent(Map<String, String> headers, EventEnvelope input, int instance) throws Exception {
        var compositeCid = input.getCorrelationId();
        if (compositeCid != null) {
            // for AsyncMultiInbox amd FutureMultiInbox, the compositeCid contains a cid and a sequence number
            var sep = compositeCid.lastIndexOf('-');
            var cid = sep == -1? compositeCid : compositeCid.substring(0, sep);
            var inbox = InboxBase.getHolder(cid);
            if (inbox != null) {
                Platform.getInstance().getVirtualThreadExecutor().submit(() -> {
                    inbox.handleEvent(input);
                });
            }
        }
        return null;
    }
}
