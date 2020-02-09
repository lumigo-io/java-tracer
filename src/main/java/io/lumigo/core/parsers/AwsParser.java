package io.lumigo.core.parsers;

import com.amazonaws.Request;
import com.amazonaws.Response;
import io.lumigo.models.ContainerHttpSpan;
import java.util.List;

public interface AwsParser {
    void parse(ContainerHttpSpan span, Request request, Response response);

    default String getParameter(Request request, String key) {

        if (request.getParameters() != null
                && request.getParameters().get(key) != null
                && ((List) request.getParameters().get(key)).size() > 0) {
            return ((List) request.getParameters().get(key)).get(0).toString();
        }
        return null;
    }
}
