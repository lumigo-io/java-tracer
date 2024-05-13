package io.lumigo.core.parsers.v1;

import com.amazonaws.Request;
import com.amazonaws.Response;
import io.lumigo.models.HttpSpan;

public class DoNothingV1Parser implements AwsSdkV1Parser {
    @Override
    public String getParserType() {
        return DoNothingV1Parser.class.getName();
    }

    @Override
    public void parse(HttpSpan span, Request request, Response response) {}
}
