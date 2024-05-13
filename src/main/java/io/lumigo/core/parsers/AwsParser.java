package io.lumigo.core.parsers;

import com.amazonaws.Request;
import com.amazonaws.Response;
import io.lumigo.models.HttpSpan;
import java.util.List;
import org.pmw.tinylog.Logger;
import software.amazon.awssdk.core.interceptor.Context;

public interface AwsParser {
    String getParserType();

    void parse(HttpSpan span, Request request, Response response);

    default void safeParse(HttpSpan span, Request request, Response response) {
        try {
            Logger.debug("Start parsing aws v1 request using: " + getParserType());
            parse(span, request, response);
            Logger.debug("Finish parsing aws v1 request using: " + getParserType());
        } catch (Throwable e) {
            Logger.error("Failed to parse extra aws v1 data using parser: " + getParserType(), e);
        }
    }

    void parseV2(HttpSpan span, Context.AfterExecution context);

    default void safeParseV2(HttpSpan span, Context.AfterExecution context) {
        try {
            Logger.debug("Start parsing aws v2 request using: " + getParserType());
            parseV2(span, context);
            Logger.debug("Finish parsing aws v2 request using: " + getParserType());
        } catch (Throwable e) {
            Logger.error(
                    "Failed to parse extra aws sdk v2 data using parser: " + getParserType(), e);
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
