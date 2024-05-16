package io.lumigo.core.parsers.v2;

import io.lumigo.models.HttpSpan;
import org.pmw.tinylog.Logger;
import software.amazon.awssdk.core.interceptor.Context;

public interface AwsSdkV2Parser {
    String getParserType();

    void parse(HttpSpan span, Context.AfterExecution context);

    default void safeParse(HttpSpan span, Context.AfterExecution context) {
        try {
            Logger.debug("Start parsing aws v2 request using: " + getParserType());
            parse(span, context);
            Logger.debug("Finish parsing aws v2 request using: " + getParserType());
        } catch (Throwable e) {
            Logger.error(
                    "Failed to parse extra aws sdk v2 data using parser: " + getParserType(), e);
        }
    }
}
