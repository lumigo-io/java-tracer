package io.lumigo.core.parsers;

import com.amazonaws.Request;
import com.amazonaws.Response;
import io.lumigo.models.ContainerHttpSpan;

public class DefaultParser implements AwsParser {
    @Override
    public void parse(ContainerHttpSpan span, Request request, Response response) {}
}
