package com.lumigo.models;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Builder(toBuilder = true)
@Data(staticConstructor = "of")
public class Span {
    private String name;
    private long started;
    private long ended;
    private String runtime;
    private String id;
    private String type;
    private String memoryAllocated;
    private String logStreamName;
    private String logGroupName;
    private String traceRoot;
    private String transactionId;
    private String requestId;
    private String account;
    private String traceIdSuffix;
    private Map<String, String> triggerBy;
    private int maxFinishTime;
    private String event;
    private String envs;
    private String region;
    private Long reporter_rtt;
    private Error error;
    private String dynamodbMethod;
    private String token;
    private String return_value;
    private Info info;


    @Builder(toBuilder = true)
    @Data(staticConstructor = "of")
    public static class Info {
        private Tracer tracer;
        private TraceId traceId;
        private String logStreamName;
        private String name;
        private String logGroupName;
        private String triggeredBy;
        private HttpInfo httpInfo;
    }

    @Builder(toBuilder = true)
    @Data(staticConstructor = "of")
    public static class HttpInfo {
        private String host;
        private Http response;
        private Http request;

    }

    @Builder(toBuilder = true)
    @Data(staticConstructor = "of")
    public static class Http {
        private String body;
        private String headers;
    }

    @Builder(toBuilder = true)
    @Data(staticConstructor = "of")
    public static class Tracer {
        private String version;
    }

    @Builder(toBuilder = true)
    @Data(staticConstructor = "of")
    public static class TraceId {
        private String Root;
    }

    @Builder(toBuilder = true)
    @Data(staticConstructor = "of")
    public static class Error {
        private String type;
        private String message;
        private String stacktrace;
    }
}


