package io.lumigo.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Builder(toBuilder = true)
@Data(staticConstructor = "of")
public class ContainerSpan {

    private long started;
    private long ended;
    private String type;
    private String invocationId;
    private ContainerTracerInformation tracerInformation;
    private ECSContainerEnvironmentInformation ecsContainerEnvironmentInformation;

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
        private String envs;
    }
}
