package io.lumigo.core.parsers;

import com.amazonaws.Request;
import com.amazonaws.Response;
import io.lumigo.models.HttpSpan;
import java.util.List;
import software.amazon.awssdk.core.interceptor.Context;

public interface AwsParser {
    void parse(HttpSpan span, Request request, Response response);

    void parseV2(HttpSpan span, Context.AfterExecution context);

    default String getParameter(Request request, String key) {

        if (request.getParameters() != null
                && request.getParameters().get(key) != null
                && ((List) request.getParameters().get(key)).size() > 0) {
            return ((List) request.getParameters().get(key)).get(0).toString();
        }
        return null;
    }
}
