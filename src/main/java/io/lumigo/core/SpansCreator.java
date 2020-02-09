package io.lumigo.core;

import com.amazonaws.Request;
import com.amazonaws.Response;
import io.lumigo.core.configuration.Configuration;
import io.lumigo.core.parsers.AwsParserFactory;
import io.lumigo.core.utils.JsonUtils;
import io.lumigo.core.utils.StringUtils;
import io.lumigo.models.ContainerHttpSpan;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.pmw.tinylog.Logger;

public class SpansCreator {
    private static final int MAX_STRING_SIZE = Configuration.getInstance().maxSpanFieldSize();

    public ContainerHttpSpan createHttpSpan(
            Long startTime, HttpUriRequest request, HttpResponse response) {
        Configuration configuration = Configuration.getInstance();

        ContainerHttpSpan.ContainerHttpSpanRequest containerHttpSpanRequest =
                ContainerHttpSpan.ContainerHttpSpanRequest.builder()
                        .headers(callIfVerbose(() -> extractHeaders(request.getAllHeaders())))
                        .uri(callIfVerbose(() -> request.getURI().toString()))
                        .method(request.getMethod())
                        .body(callIfVerbose(() -> extractBodyFromRequest(request)))
                        .build();

        ContainerHttpSpan.ContainerHttpSpanResponse containerHttpSpanResponse =
                ContainerHttpSpan.ContainerHttpSpanResponse.builder()
                        .headers(callIfVerbose(() -> extractHeaders(response.getAllHeaders())))
                        .body(callIfVerbose(() -> extractBodyFromResponse(response)))
                        .statusCode(response.getStatusLine().getStatusCode())
                        .build();

        return ContainerHttpSpan.builder()
                .host(request.getURI().getHost())
                .started(startTime)
                .ended(System.currentTimeMillis())
                .tracerInformation(
                        ContainerHttpSpan.ContainerTracerInformation.builder()
                                .runtime(configuration.javaVersion())
                                .token(configuration.getLumigoToken())
                                .version(configuration.getLumigoTracerVersion())
                                .build())
                .request(containerHttpSpanRequest)
                .response(containerHttpSpanResponse)
                .build();
    }

    public ContainerHttpSpan createHttpSpan(
            Long startTime, Request<?> request, Response<?> response) {
        Configuration configuration = Configuration.getInstance();

        ContainerHttpSpan.ContainerHttpSpanRequest containerHttpSpanRequest =
                ContainerHttpSpan.ContainerHttpSpanRequest.builder()
                        .headers(callIfVerbose(() -> extractHeaders(request.getHeaders())))
                        .uri(callIfVerbose(() -> request.getEndpoint().toString()))
                        .method(request.getHttpMethod().name())
                        .body(callIfVerbose(() -> extractBodyFromRequest(request)))
                        .build();

        ContainerHttpSpan.ContainerHttpSpanResponse containerHttpSpanResponse =
                ContainerHttpSpan.ContainerHttpSpanResponse.builder()
                        .headers(
                                callIfVerbose(
                                        () ->
                                                extractHeaders(
                                                        response.getHttpResponse().getHeaders())))
                        .body(callIfVerbose(() -> extractBodyFromResponse(response)))
                        .statusCode(response.getHttpResponse().getStatusCode())
                        .build();

        ContainerHttpSpan httpSpan =
                ContainerHttpSpan.builder()
                        .host(request.getEndpoint().getHost())
                        .started(startTime)
                        .ended(System.currentTimeMillis())
                        .tracerInformation(
                                ContainerHttpSpan.ContainerTracerInformation.builder()
                                        .runtime(configuration.javaVersion())
                                        .token(configuration.getLumigoToken())
                                        .version(configuration.getLumigoTracerVersion())
                                        .build())
                        .request(containerHttpSpanRequest)
                        .response(containerHttpSpanResponse)
                        .build();
        AwsParserFactory.getParser(request.getServiceName()).parse(httpSpan, request, response);
        return httpSpan;
    }

    private static String extractHeaders(Map<String, String> headers) {
        return JsonUtils.getObjectAsJsonString(headers);
    }

    private static String extractHeaders(Header[] headers) {
        Map<String, String> headersMap = new HashMap<>();
        if (headers != null) {
            for (Header header : headers) {
                headersMap.put(header.getName(), header.getValue());
            }
        }
        return extractHeaders(headersMap);
    }

    protected static String extractBodyFromRequest(Request<?> request) {
        return extractBodyFromRequest(request.getContent());
    }

    protected static String extractBodyFromRequest(HttpUriRequest request) throws Exception {
        if (request instanceof HttpEntityEnclosingRequestBase) {
            HttpEntity entity = ((HttpEntityEnclosingRequestBase) request).getEntity();
            if (entity != null) {
                return extractBodyFromRequest(entity.getContent());
            }
        }
        return null;
    }

    protected static String extractBodyFromRequest(InputStream stream) {
        return StringUtils.extractStringForStream(stream, MAX_STRING_SIZE);
    }

    protected static String extractBodyFromResponse(HttpResponse response) throws IOException {
        return StringUtils.extractStringForStream(
                response.getEntity() != null ? response.getEntity().getContent() : null,
                MAX_STRING_SIZE);
    }

    protected static String extractBodyFromResponse(Response response) {
        return response.getAwsResponse() != null
                ? JsonUtils.getObjectAsJsonString(response.getAwsResponse())
                : null;
    }

    protected static <T> T callIfVerbose(Callable<T> method) {
        if (!Configuration.getInstance().isLumigoVerboseMode()) {
            return null;
        }
        try {
            return method.call();
        } catch (Exception e) {
            Logger.error(e, "Failed to call method");
            return null;
        }
    }
}
