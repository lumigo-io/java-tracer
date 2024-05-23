package io.lumigo.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.lumigo.core.configuration.Configuration;
import io.lumigo.core.utils.JsonUtils;
import io.lumigo.core.utils.SecretScrubber;
import io.lumigo.core.utils.StringUtils;
import java.util.List;
import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Builder(toBuilder = true)
@Data(staticConstructor = "of")
public class Span implements BaseSpan {
    private String name;
    private long started;
    private long ended;
    private String runtime;
    private String id;
    private String type;
    private String memoryAllocated;
    private String transactionId;
    private String requestId;
    private String account;
    private long maxFinishTime;
    private String event;
    private String envs;
    private String region;
    private Long reporter_rtt;
    private Error error;
    private String token;
    private String return_value;
    private Info info;
    private String readiness;
    private String parentId;

    @AllArgsConstructor
    @Builder(toBuilder = true)
    @Data(staticConstructor = "of")
    public static class Info {
        private Tracer tracer;
        private TraceId traceId;
        private String logStreamName;
        private String logGroupName;
        private String triggeredBy;
        private String arn;
        private String httpMethod;
        private String resource;
        private String api;
        private String stage;
        private String messageId;
        private List<String> messageIds;
        private long approxEventCreationTime;
    }

    @AllArgsConstructor
    @Builder(toBuilder = true)
    @Data(staticConstructor = "of")
    public static class Tracer {
        private String version;
    }

    @AllArgsConstructor
    @Builder(toBuilder = true)
    @Data(staticConstructor = "of")
    public static class TraceId {
        @JsonProperty("Root")
        private String root;
    }

    @AllArgsConstructor
    @Builder(toBuilder = true)
    @Data(staticConstructor = "of")
    public static class Error {
        private String type;
        private String message;
        private String stacktrace;
    }

    public enum READINESS {
        WARM,
        COLD;

        public String toString() {
            return name().toLowerCase(Locale.ENGLISH);
        }
    }

    @Override
    public Span scrub(SecretScrubber scrubber) {
        this.setEnvs(
                JsonUtils.getObjectAsJsonString(scrubber.scrubStringifiedObject(this.getEnvs())));
        this.setEvent(
                JsonUtils.getObjectAsJsonString(scrubber.scrubStringifiedObject(this.getEvent())));
        this.setReturn_value(scrubber.scrubStringifiedObject(this.getReturn_value()));
        return this;
    }

    @Override
    public Span reduceSize(int maxFieldSize) {
        this.setEnvs(
                StringUtils.getMaxSizeString(
                        this.getEnvs(), Configuration.getInstance().maxSpanFieldSize()));
        this.setReturn_value(StringUtils.getMaxSizeString(this.getReturn_value(), maxFieldSize));
        this.setEvent(StringUtils.getMaxSizeString(this.getEvent(), maxFieldSize));
        return this;
    }
}
