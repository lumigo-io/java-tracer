package io.lumigo.core.parsers.event;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;

public interface EventParserFactory {
    static Object parseEvent(Object event) {
        if (event instanceof APIGatewayProxyRequestEvent) {
            return new APIGWEventParser().parse((APIGatewayProxyRequestEvent) event);
        } else {
            return event;
        }
    }
}
