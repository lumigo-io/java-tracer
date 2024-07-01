package io.lumigo.core.parsers.v1;

import com.amazonaws.Request;
import com.amazonaws.Response;
import io.lumigo.models.HttpSpan;
import java.util.List;
import org.pmw.tinylog.Logger;

public interface AwsSdkV1Parser {
    String getParserType();

    void parse(HttpSpan span, Request request, Response response);

    default void safeParse(HttpSpan span, Request request, Response response) {
        try {
            Logger.debug("Start parsing aws v1 request using: " + getParserType());
            parse(span, request, response);
            Logger.debug("Finish parsing aws v1 request using: " + getParserType());
        } catch (Throwable e) {
            Logger.error(e, "Failed to parse extra aws v1 data using parser: " + getParserType());
        }
    }

    default String getParameter(Request request, String key) {

        if (request.getParameters() != null
                && request.getParameters().get(key) != null
                && ((List) request.getParameters().get(key)).size() > 0) {
            return ((List) request.getParameters().get(key)).get(0).toString();
        }
        return null;
    }
}
