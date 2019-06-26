package io.lumigo.core.parsers;

import com.amazonaws.Request;
import com.amazonaws.Response;
import io.lumigo.models.HttpSpan;

public interface IAwsParser {
    void parse(HttpSpan span, Request request, Response response);
}
