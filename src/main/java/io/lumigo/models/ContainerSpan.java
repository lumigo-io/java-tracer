package io.lumigo.models;

import io.lumigo.core.configuration.Configuration;
import io.lumigo.core.utils.EnvUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Builder(toBuilder = true)
@Data(staticConstructor = "of")
public class ContainerSpan {

    private static final String ECS_CLUSTER = "ECS_CLUSTER";
    private static final String AWS_ACCOUNT = "AWS_ACCOUNT";

    private long started;
    private long ended;
    @Builder.Default private String type = "ContainerInvocationEnded";
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
        @Builder.Default private String clusterName = new EnvUtil().getEnv(ECS_CLUSTER);

        @Builder.Default
        private String region = new EnvUtil().getEnv(Configuration.AWS_DEFAULT_REGION);

        @Builder.Default private String accountId = new EnvUtil().getEnv(AWS_ACCOUNT);
        private String envs;
    }
}
