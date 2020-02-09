package io.lumigo.models;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Builder(toBuilder = true)
@Data(staticConstructor = "of")
public class ContainerHttpSpan {

    private long started;
    private long ended;
    private Long invocationId;

    private ContainerTracerInformation tracerInformation;
    private ECSContainerEnvironmentInformation ecsContainerEnvironmentInformation;
    private ContainerHttpSpanRequest request;
    private ContainerHttpSpanResponse response;
    private String host;
    private List<String> messageIds;
    private String resourceName;
    private String targetArn;

    @AllArgsConstructor
    @Builder(toBuilder = true)
    @Data(staticConstructor = "of")
    public static class ContainerTracerInformation {
        private String version;
        private String token;
        private String runtime;
    }

    @AllArgsConstructor
    @Builder(toBuilder = true)
    @Data(staticConstructor = "of")
    public static class ECSContainerEnvironmentInformation {
        private String clusterName;
        private String region;
        private String accountId;
    }

    @AllArgsConstructor
    @Builder(toBuilder = true)
    @Data(staticConstructor = "of")
    public static class ContainerHttpSpanRequest {
        private String body;
        private String headers;
        private String uri;
        private String method;
    }

    @AllArgsConstructor
    @Builder(toBuilder = true)
    @Data(staticConstructor = "of")
    public static class ContainerHttpSpanResponse {
        private String body;
        private String headers;
        private int statusCode;
    }
}
