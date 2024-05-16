package io.lumigo.core.parsers.v2;

import io.lumigo.models.HttpSpan;
import software.amazon.awssdk.core.interceptor.Context;

public class DoNothingV2Parser implements AwsSdkV2Parser {
    @Override
    public String getParserType() {
        return DoNothingV2Parser.class.getName();
    }

    @Override
    public void parse(HttpSpan span, Context.AfterExecution context) {}
}
