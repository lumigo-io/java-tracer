package io.lumigo.core.parsers.event;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import io.lumigo.core.utils.EnvUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import lombok.Setter;

public class APIGWEventParser implements IEventParser<APIGatewayProxyRequestEvent> {

    private static final String LUMIGO_API_GW_PREFIX_KEYS_HEADERS_DELETE_KEYS =
            "LUMIGO_API_GW_PREFIX_KEYS_HEADERS_DELETE_KEYS";

    @Setter private EnvUtil envUtil = new EnvUtil();

    private String[] HEADERS_REMOVE_KEYS_DEFUALT = {
        "cookie", "x-amz", "accept", "cloudfront", "via", "x-forwarded", "sec-"
    };
    private String[] HEADERS_REMOVE_KEYS =
            envUtil.getStringArrayEnv(
                    LUMIGO_API_GW_PREFIX_KEYS_HEADERS_DELETE_KEYS, HEADERS_REMOVE_KEYS_DEFUALT);

    @Override
    public Object parse(APIGatewayProxyRequestEvent event) {
        return APIGWEvent.builder()
                .path(event.getPath())
                .body(event.getBody())
                .resource(event.getResource())
                .httpMethod(event.getHttpMethod())
                .stageVariables(event.getStageVariables())
                .pathParameters(event.getPathParameters())
                .queryStringParameters(event.getQueryStringParameters())
                .authorizer(event.getRequestContext().getAuthorizer())
                .headers(removeHeadersKeys(event.getHeaders()))
                .build();
    }

    private Map<String, String> removeHeadersKeys(Map<String, String> headers) {

        Map<String, String> res = new HashMap<>();
        for (Map.Entry<String, String> e : headers.entrySet()) {
            if (Stream.of(HEADERS_REMOVE_KEYS)
                    .noneMatch(s -> e.getKey().toLowerCase().startsWith(s))) {
                res.put(e.getKey(), e.getValue());
            }
        }
        return res;
    }
}
