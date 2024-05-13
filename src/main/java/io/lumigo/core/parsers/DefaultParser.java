package io.lumigo.core.parsers;

import com.amazonaws.Request;
import com.amazonaws.Response;
import io.lumigo.models.HttpSpan;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;

public class DefaultParser implements AwsParser {
    @Override
    public void parse(HttpSpan span, Request request, Response response) {}

    @Override
    public void parseV2(HttpSpan span, Context.AfterExecution context) {}
}
