package io.lumigo.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.lumigo.core.utils.SecretScrubber;
import io.lumigo.core.utils.StringUtils;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Builder(toBuilder = true)
@Data(staticConstructor = "of")
public class HttpSpan implements BaseSpan {
    private Long started;
    private Long ended;
    private String id;
    private String type;
    private String transactionId;
    private String account;
    private String region;
    private String token;
    private Info info;
    private String parentId;

    @Builder(toBuilder = true)
    @Data(staticConstructor = "of")
    public static class Info {
        private Tracer tracer;
        private TraceId traceId;
        private HttpInfo httpInfo;
        private String messageId;
        private List<String> messageIds;
        private String resourceName;
        private String targetArn;

        public Info(
                Tracer tracer,
                TraceId traceId,
                HttpInfo httpInfo,
                String messageId,
                List<String> messageIds,
                String resourceName,
                String targetArn) {
            this.tracer = tracer;
            this.traceId = traceId;
            this.httpInfo = httpInfo;
            this.messageId = messageId;
            if (messageIds != null) {
                this.messageIds = Collections.unmodifiableList(messageIds);
            }
            this.resourceName = resourceName;
            this.targetArn = targetArn;
        }
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
    public static class Tracer {
        private String version;
    }

    @AllArgsConstructor
    @Builder(toBuilder = true)
    @Data(staticConstructor = "of")
    public static class HttpInfo {
        private String host;
        private HttpData request;
        private HttpData response;
    }

    @AllArgsConstructor
    @Builder(toBuilder = true)
    @Data(staticConstructor = "of")
    public static class HttpData {
        private String headers;
        private String body;
        private String uri;
        private Integer statusCode;
        private String method;
    }

    @Override
    public BaseSpan scrub(SecretScrubber scrubber) {
        this.getInfo()
                .getHttpInfo()
                .getRequest()
                .setHeaders(
                        scrubber.scrubStringifiedObject(
                                this.getInfo().getHttpInfo().getRequest().getHeaders()));
        this.getInfo()
                .getHttpInfo()
                .getRequest()
                .setBody(
                        scrubber.scrubStringifiedObject(
                                this.getInfo().getHttpInfo().getRequest().getBody()));
        this.getInfo()
                .getHttpInfo()
                .getResponse()
                .setHeaders(
                        scrubber.scrubStringifiedObject(
                                this.getInfo().getHttpInfo().getResponse().getHeaders()));
        this.getInfo()
                .getHttpInfo()
                .getResponse()
                .setBody(
                        scrubber.scrubStringifiedObject(
                                this.getInfo().getHttpInfo().getResponse().getBody()));

        return this;
    }

    @Override
    public BaseSpan reduceSize(int maxFieldSize) {
        this.getInfo()
                .getHttpInfo()
                .getRequest()
                .setHeaders(
                        StringUtils.getMaxSizeString(
                                this.getInfo().getHttpInfo().getRequest().getHeaders(),
                                maxFieldSize));
        this.getInfo()
                .getHttpInfo()
                .getRequest()
                .setBody(
                        StringUtils.getMaxSizeString(
                                this.getInfo().getHttpInfo().getRequest().getBody(), maxFieldSize));
        this.getInfo()
                .getHttpInfo()
                .getResponse()
                .setHeaders(
                        StringUtils.getMaxSizeString(
                                this.getInfo().getHttpInfo().getResponse().getHeaders(),
                                maxFieldSize));
        this.getInfo()
                .getHttpInfo()
                .getResponse()
                .setBody(
                        StringUtils.getMaxSizeString(
                                this.getInfo().getHttpInfo().getResponse().getBody(),
                                maxFieldSize));

        return this;
    }
}
