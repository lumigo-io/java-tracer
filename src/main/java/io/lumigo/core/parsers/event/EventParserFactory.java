package io.lumigo.core.parsers.event;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import org.pmw.tinylog.Logger;

public interface EventParserFactory {
    static Object parseEvent(Object event) {
        try {
            if (event instanceof APIGatewayProxyRequestEvent) {
                return new APIGWEventParser().parse((APIGatewayProxyRequestEvent) event);
            } else if (event instanceof SNSEvent) {
                return new SnsEventParser().parse((SNSEvent) event);
            } else if (event instanceof SQSEvent) {
                return new SqsEventParser().parse((SQSEvent) event);
            } else {
                return event;
            }
        } catch (Exception e) {
            Logger.error(e, "Fail to parse event");
        }
        return event;
    }
}
