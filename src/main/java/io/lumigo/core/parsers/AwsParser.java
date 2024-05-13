package io.lumigo.core.parsers;

import com.amazonaws.Request;
import com.amazonaws.Response;
import io.lumigo.models.HttpSpan;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;

import java.util.List;

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
