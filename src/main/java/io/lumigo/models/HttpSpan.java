package io.lumigo.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Builder(toBuilder = true)
@Data(staticConstructor = "of")
public class HttpSpan {
    private long started;
    private long ended;
    private String id;
    private String type;
    private String transactionId;
    private String account;
    private String region;
    private String token;
    private Info info;
    private String parentId;

    @AllArgsConstructor
    @Builder(toBuilder = true)
    @Data(staticConstructor = "of")
    public static class Info {
        private HttpInfo httpInfo;
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
    }
}
