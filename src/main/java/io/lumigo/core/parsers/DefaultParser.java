package io.lumigo.core.parsers;

import com.amazonaws.Request;
import com.amazonaws.Response;
import io.lumigo.models.HttpSpan;

public class DefaultParser implements AwsParser {
    @Override
    public void parse(HttpSpan span, Request request, Response response) {}
}
