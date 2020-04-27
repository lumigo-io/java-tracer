package io.lumigo.core.parsers.event;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Builder(toBuilder = true)
@Data(staticConstructor = "of")
public class APIGWEvent {
    public String path;
    public String resource;
    public String httpMethod;
    public Map<String, String> queryStringParameters;
    public Map<String, String> pathParameters;
    public String body;
    public Map<String, Object> authorizer;
    public Map<String, String> headers;
    public Map<String, String> stageVariables;
}
