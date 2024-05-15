package io.lumigo.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.util.StringInputStream;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.lumigo.core.configuration.Configuration;
import io.lumigo.core.network.Reporter;
import io.lumigo.core.utils.EnvUtil;
import io.lumigo.core.utils.JsonUtils;
import io.lumigo.handlers.LumigoConfiguration;
import io.lumigo.models.HttpSpan;
import io.lumigo.models.Span;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.*;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

class SpansContainerTest {
    private static final char ch = '*';

    private SpansContainer spansContainer = SpansContainer.getInstance();

    @Mock private EnvUtil envUtil;
    @Mock private Context context;
    @Mock Reporter reporter;
    @Mock HttpResponse httpResponse;
    @Mock StatusLine statusLine;
    @Mock HttpUriRequest httpRequest;
    @Mock Request awsRequest;
    @Mock com.amazonaws.http.HttpResponse awsHttpResponse;

    @BeforeEach
    void setUp() {
        Configuration.getInstance().init(LumigoConfiguration.builder().verbose(true).build());
        MockitoAnnotations.initMocks(this);
        spansContainer.clear();
        mockContext();
    }

    @DisplayName("Check that clear SpansContainer state working")
    @Test
    void clear() throws JsonProcessingException {
        spansContainer.init(createMockedEnv(), reporter, context, null);
        spansContainer.start();
        spansContainer.clear();

        assertNull(spansContainer.getStartFunctionSpan());
    }

    @DisplayName("Check that start span include all relevant data")
    @Test
    void createStartSpan() throws Exception {
        spansContainer.init(createMockedEnv(), reporter, context, null);
        spansContainer.start();

        Span actualSpan = spansContainer.getStartFunctionSpan();
        String expectedSpan =
                "{\n"
                        + "  \"name\": \"mocked_function_name\",\n"
                        + "  \"started\": 1557823871416,\n"
                        + "  \"ended\": 1557823871416,\n"
                        + "  \"runtime\": \"JAVA8\",\n"
                        + "  \"id\": \"3n2783hf7823hdui32_started\",\n"
                        + "  \"type\": function,\n"
                        + "  \"memoryAllocated\": \"100\",\n"
                        + "  \"transactionId\": \"3\",\n"
                        + "  \"requestId\": \"3n2783hf7823hdui32\",\n"
                        + "  \"account\": \"1111\",\n"
                        + "  \"maxFinishTime\": 100,\n"
                        + "  \"event\": null,\n"
                        + "  \"envs\": \"{\\\"AWS_REGION\\\":\\\"us-west-2\\\",\\\"_X_AMZN_TRACE_ID\\\":\\\"Root=1-2-3;Another=456;Bla=789\\\",\\\"AWS_EXECUTION_ENV\\\":\\\"JAVA8\\\"}\",\n"
                        + "  \"region\": \"us-west-2\",\n"
                        + "  \"reporter_rtt\": null,\n"
                        + "  \"error\": null,\n"
                        + "  \"token\": null,\n"
                        + "  \"return_value\": null,\n"
                        + "  \"info\": {\n"
                        + "    \"tracer\": {\n"
                        + "      \"version\": \"1.0\"\n"
                        + "    },\n"
                        + "    \"traceId\": {\n"
                        + "      \"Root\": \"1-2-3\"\n"
                        + "    },\n"
                        + "  \"logStreamName\": \"2019/05/12/[$LATEST]7f67fc1238a941749d8126be19f0cdc6\",\n"
                        + "  \"logGroupName\": \"/aws/lambda/mocked_function_name\",\n"
                        + "    \"triggeredBy\": null\n"
                        + "  }\n"
                        + "}";
        long started = actualSpan.getStarted();
        JSONAssert.assertEquals(
                expectedSpan,
                JsonUtils.getObjectAsJsonString(actualSpan),
                new CustomComparator(
                        JSONCompareMode.LENIENT,
                        new Customization("info.tracer.version", (o1, o2) -> o2 != null),
                        new Customization("started", (o1, o2) -> o2 != null),
                        new Customization("token", (o1, o2) -> o2 != null),
                        new Customization(
                                "maxFinishTime",
                                (o1, o2) -> started + 100 == Long.valueOf(o1.toString())),
                        new Customization("ended", (o1, o2) -> o2 != null)));
    }

    @DisplayName("End span which contains error")
    @Test
    void endWithException() throws Exception {
        spansContainer.init(createMockedEnv(), reporter, context, null);
        spansContainer.endWithException(new Exception("Error in code"));

        Span actualSpan = spansContainer.getEndSpan();
        String expectedSpan =
                "{\n"
                        + "  \"name\": \"mocked_function_name\",\n"
                        + "  \"started\": 1557827340189,\n"
                        + "  \"ended\": 1557827340194,\n"
                        + "  \"runtime\": \"JAVA8\",\n"
                        + "  \"id\": \"3n2783hf7823hdui32\",\n"
                        + "  \"type\": function,\n"
                        + "  \"memoryAllocated\": \"100\",\n"
                        + "  \"transactionId\": \"3\",\n"
                        + "  \"requestId\": \"3n2783hf7823hdui32\",\n"
                        + "  \"account\": \"1111\",\n"
                        + "  \"maxFinishTime\": 100,\n"
                        + "  \"event\": null,\n"
                        + "  \"envs\": \"{\\\"AWS_REGION\\\":\\\"us-west-2\\\",\\\"_X_AMZN_TRACE_ID\\\":\\\"Root=1-2-3;Another=456;Bla=789\\\",\\\"AWS_EXECUTION_ENV\\\":\\\"JAVA8\\\"}\",\n"
                        + "  \"region\": \"us-west-2\",\n"
                        + "  \"reporter_rtt\": null,\n"
                        + "  \"error\": {\n"
                        + "    \"type\": \"java.lang.Exception\",\n"
                        + "    \"message\": \"Error in code\",\n"
                        + "    \"stacktrace\": \"java.lang.Exception\"\n"
                        + "  },\n"
                        + "  \"token\": null,\n"
                        + "  \"return_value\": null,\n"
                        + "  \"info\": {\n"
                        + "    \"tracer\": {\n"
                        + "      \"version\": \"1.0\"\n"
                        + "    },\n"
                        + "    \"traceId\": {\n"
                        + "      \"Root\": \"1-2-3\"\n"
                        + "    },\n"
                        + "  \"logStreamName\": \"2019/05/12/[$LATEST]7f67fc1238a941749d8126be19f0cdc6\",\n"
                        + "  \"logGroupName\": \"/aws/lambda/mocked_function_name\",\n"
                        + "    \"triggeredBy\": null\n"
                        + "  }\n"
                        + "}";
        long started = actualSpan.getStarted();
        JSONAssert.assertEquals(
                expectedSpan,
                JsonUtils.getObjectAsJsonString(actualSpan),
                new CustomComparator(
                        JSONCompareMode.LENIENT,
                        new Customization("info.tracer.version", (o1, o2) -> o1 != null),
                        new Customization("started", (o1, o2) -> o1 != null),
                        new Customization("ended", (o1, o2) -> o1 != null),
                        new Customization("token", (o1, o2) -> o1 != null),
                        new Customization(
                                "maxFinishTime",
                                (o1, o2) -> started + 100 == Long.valueOf(o1.toString())),
                        new Customization("error.stacktrace", (o1, o2) -> o1 != null)));
    }

    @DisplayName("End span creation")
    @Test
    void end() throws Exception {
        spansContainer.init(createMockedEnv(), reporter, context, null);
        spansContainer.end();

        Span actualSpan = spansContainer.getEndSpan();
        String expectedSpan =
                "{\n"
                        + "  \"name\": \"mocked_function_name\",\n"
                        + "  \"started\": 1557827936392,\n"
                        + "  \"ended\": 1557827936392,\n"
                        + "  \"runtime\": \"JAVA8\",\n"
                        + "  \"id\": \"3n2783hf7823hdui32\",\n"
                        + "  \"type\": function,\n"
                        + "  \"memoryAllocated\": \"100\",\n"
                        + "  \"transactionId\": \"3\",\n"
                        + "  \"requestId\": \"3n2783hf7823hdui32\",\n"
                        + "  \"account\": \"1111\",\n"
                        + "  \"maxFinishTime\": 100,\n"
                        + "  \"event\": null,\n"
                        + "  \"envs\": \"{\\\"AWS_REGION\\\":\\\"us-west-2\\\",\\\"_X_AMZN_TRACE_ID\\\":\\\"Root=1-2-3;Another=456;Bla=789\\\",\\\"AWS_EXECUTION_ENV\\\":\\\"JAVA8\\\"}\",\n"
                        + "  \"region\": \"us-west-2\",\n"
                        + "  \"reporter_rtt\": null,\n"
                        + "  \"error\": null,\n"
                        + "  \"token\": null,\n"
                        + "  \"return_value\": null,\n"
                        + "  \"info\": {\n"
                        + "    \"tracer\": {\n"
                        + "      \"version\": \"1.0\"\n"
                        + "    },\n"
                        + "    \"traceId\": {\n"
                        + "      \"Root\": \"1-2-3\"\n"
                        + "    },\n"
                        + "  \"logStreamName\": \"2019/05/12/[$LATEST]7f67fc1238a941749d8126be19f0cdc6\",\n"
                        + "  \"logGroupName\": \"/aws/lambda/mocked_function_name\",\n"
                        + "    \"triggeredBy\": null\n"
                        + "  }\n"
                        + "}";
        long started = actualSpan.getStarted();
        JSONAssert.assertEquals(
                expectedSpan,
                JsonUtils.getObjectAsJsonString(actualSpan),
                new CustomComparator(
                        JSONCompareMode.LENIENT,
                        new Customization("info.tracer.version", (o1, o2) -> o1 != null),
                        new Customization("started", (o1, o2) -> o1 != null),
                        new Customization("ended", (o1, o2) -> o1 != null),
                        new Customization(
                                "maxFinishTime",
                                (o1, o2) -> started + 100 == Long.valueOf(o1.toString())),
                        new Customization("token", (o1, o2) -> o1 != null),
                        new Customization("error.stacktrace", (o1, o2) -> o1 != null)));
    }

    @DisplayName("End span creation with return value")
    @Test
    void end_with_return_value() throws Exception {
        spansContainer.init(createMockedEnv(), reporter, context, null);
        spansContainer.end("RESULT");

        Span actualSpan = spansContainer.getEndSpan();
        String expectedSpan =
                "{\n"
                        + "  \"name\": \"mocked_function_name\",\n"
                        + "  \"started\": 1557828102847,\n"
                        + "  \"ended\": 1557828102847,\n"
                        + "  \"runtime\": \"JAVA8\",\n"
                        + "  \"id\": \"3n2783hf7823hdui32\",\n"
                        + "  \"type\": function,\n"
                        + "  \"memoryAllocated\": \"100\",\n"
                        + "  \"transactionId\": \"3\",\n"
                        + "  \"requestId\": \"3n2783hf7823hdui32\",\n"
                        + "  \"account\": \"1111\",\n"
                        + "  \"maxFinishTime\": 100,\n"
                        + "  \"event\": null,\n"
                        + "  \"envs\": \"{\\\"AWS_REGION\\\":\\\"us-west-2\\\",\\\"_X_AMZN_TRACE_ID\\\":\\\"Root=1-2-3;Another=456;Bla=789\\\",\\\"AWS_EXECUTION_ENV\\\":\\\"JAVA8\\\"}\",\n"
                        + "  \"region\": \"us-west-2\",\n"
                        + "  \"reporter_rtt\": null,\n"
                        + "  \"error\": null,\n"
                        + "  \"token\": null,\n"
                        + "  \"return_value\": \"RESULT\",\n"
                        + "  \"info\": {\n"
                        + "    \"tracer\": {\n"
                        + "      \"version\": \"1.0\"\n"
                        + "    },\n"
                        + "    \"traceId\": {\n"
                        + "      \"Root\": \"1-2-3\"\n"
                        + "    },\n"
                        + "  \"logStreamName\": \"2019/05/12/[$LATEST]7f67fc1238a941749d8126be19f0cdc6\",\n"
                        + "  \"logGroupName\": \"/aws/lambda/mocked_function_name\",\n"
                        + "    \"triggeredBy\": null\n"
                        + "  }\n"
                        + "}";
        long started = actualSpan.getStarted();
        JSONAssert.assertEquals(
                expectedSpan,
                JsonUtils.getObjectAsJsonString(actualSpan),
                new CustomComparator(
                        JSONCompareMode.LENIENT,
                        new Customization("info.tracer.version", (o1, o2) -> o1 != null),
                        new Customization("started", (o1, o2) -> o1 != null),
                        new Customization("ended", (o1, o2) -> o1 != null),
                        new Customization("token", (o1, o2) -> o1 != null),
                        new Customization(
                                "maxFinishTime",
                                (o1, o2) -> started + 100 == Long.valueOf(o1.toString())),
                        new Customization("error.stacktrace", (o1, o2) -> o1 != null)));
    }

    @DisplayName("Http span creation")
    @Test
    void add_http_span() throws Exception {
        spansContainer.init(createMockedEnv(), reporter, context, null);
        when(httpRequest.getURI()).thenReturn(URI.create("https://google.com"));
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);
        when(statusLine.getStatusCode()).thenReturn(200);

        long startTime = System.currentTimeMillis();
        spansContainer.addHttpSpan(startTime, httpRequest, httpResponse);

        HttpSpan actualSpan = spansContainer.getHttpSpans().get(0);
        String expectedSpan =
                "{\n"
                        + "   \"started\":1559127760071,\n"
                        + "   \"ended\":1559127760085,\n"
                        + "   \"id\":\"cc9ceb9c-dad2-4762-8f0c-147408bdc063\",\n"
                        + "   \"type\":\"http\",\n"
                        + "   \"transactionId\":\"3\",\n"
                        + "   \"account\":\"1111\",\n"
                        + "   \"region\":\"us-west-2\",\n"
                        + "   \"token\":null,\n"
                        + "   \"info\":{\n"
                        + "      \"tracer\":{\n"
                        + "         \"version\":\"1.0\"\n"
                        + "      },\n"
                        + "      \"traceId\":{\n"
                        + "         \"Root\":\"1-2-3\"\n"
                        + "      },\n"
                        + "      \"httpInfo\":{\n"
                        + "         \"host\":\"google.com\",\n"
                        + "         \"request\":{\n"
                        + "            \"headers\":\"{}\",\n"
                        + "            \"body\":null,\n"
                        + "            \"uri\":\"https://google.com\",\n"
                        + "            \"statusCode\":null,\n"
                        + "            \"method\":null\n"
                        + "         },\n"
                        + "         \"response\":{\n"
                        + "            \"headers\":\"{}\",\n"
                        + "            \"body\":null,\n"
                        + "            \"uri\":null,\n"
                        + "            \"statusCode\":200,\n"
                        + "            \"method\":null\n"
                        + "         }\n"
                        + "      }\n"
                        + "   },\n"
                        + "   \"parentId\":\"3n2783hf7823hdui32\"\n"
                        + "}";
        JSONAssert.assertEquals(
                expectedSpan,
                JsonUtils.getObjectAsJsonString(actualSpan),
                new CustomComparator(
                        JSONCompareMode.LENIENT,
                        new Customization("info.tracer.version", (o1, o2) -> o1 != null),
                        new Customization("id", (o1, o2) -> o1 != null),
                        new Customization("started", (o1, o2) -> o1 != null),
                        new Customization("ended", (o1, o2) -> o1 != null)));
    }

    @DisplayName("AWS Http span creation with x-amzn-requestid")
    @Test
    void add_aws_http_span_with_spnid_from_header_amzn() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-amzn-requestid", "id123");

        spansContainer.init(createMockedEnv(), reporter, context, null);
        when(awsRequest.getEndpoint()).thenReturn(URI.create("https://sns.amazonaws.com"));
        when(awsRequest.getHttpMethod()).thenReturn(HttpMethodName.GET);
        when(awsHttpResponse.getStatusCode()).thenReturn(200);
        when(awsHttpResponse.getHeaders()).thenReturn(headers);
        Response<String> awsResponse = new Response<>("awsResponse", awsHttpResponse);
        long startTime = System.currentTimeMillis();
        spansContainer.addHttpSpan(startTime, awsRequest, awsResponse);

        HttpSpan actualSpan = spansContainer.getHttpSpans().get(0);
        String expectedSpan =
                "{\n"
                        + "   \"started\":1559127760071,\n"
                        + "   \"ended\":1559127760085,\n"
                        + "   \"id\":"
                        + actualSpan.getId()
                        + ",\n"
                        + "   \"type\":\"http\",\n"
                        + "   \"transactionId\":\"3\",\n"
                        + "   \"account\":\"1111\",\n"
                        + "   \"region\":\"us-west-2\",\n"
                        + "   \"token\":null,\n"
                        + "   \"info\":{\n"
                        + "      \"tracer\":{\n"
                        + "         \"version\":\"1.0\"\n"
                        + "      },\n"
                        + "      \"traceId\":{\n"
                        + "         \"Root\":\"1-2-3\"\n"
                        + "      },\n"
                        + "      \"httpInfo\":{\n"
                        + "         \"host\":\"sns.amazonaws.com\",\n"
                        + "         \"request\":{\n"
                        + "            \"headers\":\"{}\",\n"
                        + "            \"body\":null,\n"
                        + "            \"uri\":\"https://sns.amazonaws.com\",\n"
                        + "            \"statusCode\":null,\n"
                        + "            \"method\":GET\n"
                        + "         },\n"
                        + "         \"response\":{\n"
                        + "            \"headers\":\"{\\\"x-amzn-requestid\\\":\\\"id123\\\"}\",\n"
                        + "            \"body\":\"awsResponse\",\n"
                        + "            \"uri\":null,\n"
                        + "            \"statusCode\":200,\n"
                        + "            \"method\":null\n"
                        + "         }\n"
                        + "      }\n"
                        + "   },\n"
                        + "   \"parentId\":\"3n2783hf7823hdui32\"\n"
                        + "}";
        JSONAssert.assertEquals(
                expectedSpan,
                JsonUtils.getObjectAsJsonString(actualSpan),
                new CustomComparator(
                        JSONCompareMode.LENIENT,
                        new Customization("info.tracer.version", (o1, o2) -> o1 != null),
                        new Customization("id", (o1, o2) -> o1.equals("id123")),
                        new Customization("started", (o1, o2) -> o1 != null),
                        new Customization("ended", (o1, o2) -> o1 != null)));
    }

    @DisplayName("AWS Http span creation with x-amz-requestid")
    @Test
    void add_aws_http_span_with_spnid_from_header_amz() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-amz-requestid", "id123");

        spansContainer.init(createMockedEnv(), reporter, context, null);
        when(awsRequest.getEndpoint()).thenReturn(URI.create("https://sns.amazonaws.com"));
        when(awsRequest.getHttpMethod()).thenReturn(HttpMethodName.GET);
        when(awsHttpResponse.getStatusCode()).thenReturn(200);
        when(awsHttpResponse.getHeaders()).thenReturn(headers);
        Response<String> awsResponse = new Response<>("awsResponse", awsHttpResponse);
        long startTime = System.currentTimeMillis();
        spansContainer.addHttpSpan(startTime, awsRequest, awsResponse);

        HttpSpan actualSpan = spansContainer.getHttpSpans().get(0);
        String expectedSpan =
                "{\n"
                        + "   \"started\":1559127760071,\n"
                        + "   \"ended\":1559127760085,\n"
                        + "   \"id\":\"cc9ceb9c-dad2-4762-8f0c-147408bdc063\",\n"
                        + "   \"type\":\"http\",\n"
                        + "   \"transactionId\":\"3\",\n"
                        + "   \"account\":\"1111\",\n"
                        + "   \"region\":\"us-west-2\",\n"
                        + "   \"token\":null,\n"
                        + "   \"info\":{\n"
                        + "      \"tracer\":{\n"
                        + "         \"version\":\"1.0\"\n"
                        + "      },\n"
                        + "      \"traceId\":{\n"
                        + "         \"Root\":\"1-2-3\"\n"
                        + "      },\n"
                        + "      \"httpInfo\":{\n"
                        + "         \"host\":\"sns.amazonaws.com\",\n"
                        + "         \"request\":{\n"
                        + "            \"headers\":\"{}\",\n"
                        + "            \"body\":null,\n"
                        + "            \"uri\":\"https://sns.amazonaws.com\",\n"
                        + "            \"statusCode\":null,\n"
                        + "            \"method\":GET\n"
                        + "         },\n"
                        + "         \"response\":{\n"
                        + "            \"headers\":\"{\\\"x-amz-requestid\\\":\\\"id123\\\"}\",\n"
                        + "            \"body\":\"awsResponse\",\n"
                        + "            \"uri\":null,\n"
                        + "            \"statusCode\":200,\n"
                        + "            \"method\":null\n"
                        + "         }\n"
                        + "      }\n"
                        + "   },\n"
                        + "   \"parentId\":\"3n2783hf7823hdui32\"\n"
                        + "}";
        JSONAssert.assertEquals(
                expectedSpan,
                JsonUtils.getObjectAsJsonString(actualSpan),
                new CustomComparator(
                        JSONCompareMode.LENIENT,
                        new Customization("info.tracer.version", (o1, o2) -> o1 != null),
                        new Customization("id", (o1, o2) -> o1.equals("id123")),
                        new Customization("started", (o1, o2) -> o1 != null),
                        new Customization("ended", (o1, o2) -> o1 != null)));
    }

    @DisplayName("AWS SDK V2 request")
    @Test
    void add_aws_sdk_v2_http_span() throws Exception {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("x-amz-requestid", Collections.singletonList("id123"));

        PublishRequest request = PublishRequest.builder().topicArn("topic").build();
        PublishResponse response =
                PublishResponse.builder().messageId("fee47356-6f6a-58c8-96dc-26d8aaa4631a").build();

        SdkHttpRequest sdkHttpRequest =
                SdkHttpRequest.builder()
                        .uri(new URI("https://sns.amazonaws.com"))
                        .headers(headers)
                        .method(SdkHttpMethod.GET)
                        .build();

        SdkHttpResponse sdkHttpResponse =
                SdkHttpResponse.builder().headers(headers).statusCode(200).build();

        software.amazon.awssdk.core.interceptor.Context.AfterExecution requestContext =
                InterceptorContext.builder()
                        .request(request)
                        .httpRequest(sdkHttpRequest)
                        .response(response)
                        .httpResponse(sdkHttpResponse)
                        .build();
        ExecutionAttributes executionAttributes =
                ExecutionAttributes.builder()
                        .put(SdkExecutionAttribute.SERVICE_NAME, "Sns")
                        .build();

        spansContainer.init(createMockedEnv(), reporter, context, null);
        long startTime = System.currentTimeMillis();

        spansContainer.addHttpSpan(startTime, requestContext, executionAttributes);

        HttpSpan actualSpan = spansContainer.getHttpSpans().get(0);
        String expectedSpan =
                "{\n"
                        + "   \"started\":1559127760071,\n"
                        + "   \"ended\":1559127760085,\n"
                        + "   \"id\":\"cc9ceb9c-dad2-4762-8f0c-147408bdc063\",\n"
                        + "   \"type\":\"http\",\n"
                        + "   \"transactionId\":\"3\",\n"
                        + "   \"account\":\"1111\",\n"
                        + "   \"region\":\"us-west-2\",\n"
                        + "   \"token\":null,\n"
                        + "   \"info\":{\n"
                        + "      \"tracer\":{\n"
                        + "         \"version\":\"1.0\"\n"
                        + "      },\n"
                        + "      \"traceId\":{\n"
                        + "         \"Root\":\"1-2-3\"\n"
                        + "      },\n"
                        + "      \"httpInfo\":{\n"
                        + "         \"host\":\"sns.amazonaws.com\",\n"
                        + "         \"request\":{\n"
                        + "            \"headers\":\"{\\\"x-amz-requestid\\\":[\\\"id123\\\"]}\",\n"
                        + "            \"body\":null,\n"
                        + "            \"uri\":\"https://sns.amazonaws.com\",\n"
                        + "            \"statusCode\":null,\n"
                        + "            \"method\":GET\n"
                        + "         },\n"
                        + "         \"response\":{\n"
                        + "            \"headers\":\"{\\\"x-amz-requestid\\\":[\\\"id123\\\"]}\",\n"
                        + "            \"body\":\"{\\\"messageId\\\":\\\"fee47356-6f6a-58c8-96dc-26d8aaa4631a\\\",\\\"sequenceNumber\\\":null}\", \n"
                        + "            \"uri\":null,\n"
                        + "            \"statusCode\":200,\n"
                        + "            \"method\":null\n"
                        + "         }\n"
                        + "      }\n"
                        + "   },\n"
                        + "   \"parentId\":\"3n2783hf7823hdui32\"\n"
                        + "}";

        JSONAssert.assertEquals(
                expectedSpan,
                JsonUtils.getObjectAsJsonString(actualSpan),
                new CustomComparator(
                        JSONCompareMode.LENIENT,
                        new Customization("info.tracer.version", (o1, o2) -> o1 != null),
                        new Customization("id", (o1, o2) -> o1.equals("id123")),
                        new Customization("started", (o1, o2) -> o1 != null),
                        new Customization("ended", (o1, o2) -> o1 != null)));
    }

    @DisplayName("Extract body from request")
    @Test
    void test_extract_body_from_request() throws Exception {
        assertEquals(
                "awsResponse", SpansContainer.extractBodyFromRequest(new HttpRequestMockCls()));
    }

    @DisplayName("Extract body from un reset request, should be null")
    @Test
    void test_extract_Body_From_Request() throws Exception {
        assertNull(SpansContainer.extractBodyFromRequest(new HttpRequestMockUnResetStream()));
    }

    @DisplayName("Extract body from request aws")
    @Test
    void test_extract_body_from_aws_request() throws UnsupportedEncodingException {
        when(awsRequest.getContent()).thenReturn(new StringInputStream("awsResponse"));
        assertEquals("awsResponse", SpansContainer.extractBodyFromRequest(awsRequest));
    }

    @DisplayName("Validate verbose with exception return null")
    @Test
    void test_validate_verbose_exception() {
        assertNull(
                SpansContainer.callIfVerbose(
                        () -> {
                            throw new RuntimeException();
                        }));
    }

    @DisplayName("Check reduce of function span size, less than 1024")
    @Test
    void test_reduce_function_span_size_less_than_1024() {
        Span span =
                Span.builder()
                        .envs(createStringOfSize(100))
                        .event(createStringOfSize(100))
                        .return_value(createStringOfSize(100))
                        .build();

        span = (Span) SpansContainer.getInstance().reduceSpanSize(span, false);

        Assert.assertEquals("Wrong reduce size", 100, span.getEnvs().length());
        Assert.assertEquals("Wrong reduce size", 100, span.getEvent().length());
        Assert.assertEquals("Wrong reduce size", 100, span.getReturn_value().length());
    }

    @DisplayName("Check reduce of function span size, more than 1024")
    @Test
    void test_reduce_function_span_size_more_than_1024() {
        Span span =
                Span.builder()
                        .envs(createStringOfSize(2000))
                        .event(createStringOfSize(2000))
                        .return_value(createStringOfSize(2000))
                        .build();

        span = (Span) SpansContainer.getInstance().reduceSpanSize(span, false);

        Assert.assertEquals("Wrong reduce size", 1024, span.getEnvs().length());
        Assert.assertEquals("Wrong reduce size", 1024, span.getEvent().length());
        Assert.assertEquals("Wrong reduce size", 1024, span.getReturn_value().length());
    }

    @DisplayName("Check reduce of function span with error size, more than 1024")
    @Test
    void test_reduce_function_span_size_more_than_1024_with_error() {
        Span span =
                Span.builder()
                        .envs(createStringOfSize(2000))
                        .event(createStringOfSize(2000))
                        .return_value(createStringOfSize(2000))
                        .build();

        span = (Span) SpansContainer.getInstance().reduceSpanSize(span, true);

        Assert.assertEquals("Wrong reduce size", 1024, span.getEnvs().length());
        Assert.assertEquals("Wrong reduce size", 2000, span.getEvent().length());
        Assert.assertEquals("Wrong reduce size", 2000, span.getReturn_value().length());
    }

    @DisplayName("Check reduce of http span size, less than 1024")
    @Test
    void test_reduce_http_span_size_less_than_1024() {
        HttpSpan.HttpData request =
                HttpSpan.HttpData.builder()
                        .headers(createStringOfSize(100))
                        .body(createStringOfSize(100))
                        .build();
        HttpSpan.HttpData response =
                HttpSpan.HttpData.builder()
                        .headers(createStringOfSize(100))
                        .body(createStringOfSize(100))
                        .build();
        HttpSpan httpSpan =
                HttpSpan.builder()
                        .info(
                                HttpSpan.Info.builder()
                                        .httpInfo(
                                                HttpSpan.HttpInfo.builder()
                                                        .request(request)
                                                        .response(response)
                                                        .build())
                                        .build())
                        .build();

        httpSpan = (HttpSpan) SpansContainer.getInstance().reduceSpanSize(httpSpan, false);

        Assert.assertEquals(
                "Wrong reduce size",
                100,
                httpSpan.getInfo().getHttpInfo().getRequest().getHeaders().length());
        Assert.assertEquals(
                "Wrong reduce size",
                100,
                httpSpan.getInfo().getHttpInfo().getRequest().getBody().length());
        Assert.assertEquals(
                "Wrong reduce size",
                100,
                httpSpan.getInfo().getHttpInfo().getResponse().getBody().length());
        Assert.assertEquals(
                "Wrong reduce size",
                100,
                httpSpan.getInfo().getHttpInfo().getResponse().getBody().length());
    }

    @DisplayName("Check reduce of http span size, more than 1024")
    @Test
    void test_reduce_http_span_size_more_than_1024() {
        HttpSpan.HttpData request =
                HttpSpan.HttpData.builder()
                        .headers(createStringOfSize(2000))
                        .body(createStringOfSize(2000))
                        .build();
        HttpSpan.HttpData response =
                HttpSpan.HttpData.builder()
                        .headers(createStringOfSize(2000))
                        .body(createStringOfSize(2000))
                        .build();
        HttpSpan httpSpan =
                HttpSpan.builder()
                        .info(
                                HttpSpan.Info.builder()
                                        .httpInfo(
                                                HttpSpan.HttpInfo.builder()
                                                        .request(request)
                                                        .response(response)
                                                        .build())
                                        .build())
                        .build();

        httpSpan = (HttpSpan) SpansContainer.getInstance().reduceSpanSize(httpSpan, false);

        Assert.assertEquals(
                "Wrong reduce size",
                1024,
                httpSpan.getInfo().getHttpInfo().getRequest().getHeaders().length());
        Assert.assertEquals(
                "Wrong reduce size",
                1024,
                httpSpan.getInfo().getHttpInfo().getRequest().getBody().length());
        Assert.assertEquals(
                "Wrong reduce size",
                1024,
                httpSpan.getInfo().getHttpInfo().getResponse().getBody().length());
        Assert.assertEquals(
                "Wrong reduce size",
                1024,
                httpSpan.getInfo().getHttpInfo().getResponse().getBody().length());
    }

    @DisplayName("Check reduce of http span with error size, more than 1024")
    @Test
    void test_reduce_http_span_size_more_than_1024_with_error() {
        HttpSpan.HttpData request =
                HttpSpan.HttpData.builder()
                        .headers(createStringOfSize(2000))
                        .body(createStringOfSize(2000))
                        .build();
        HttpSpan.HttpData response =
                HttpSpan.HttpData.builder()
                        .headers(createStringOfSize(2000))
                        .body(createStringOfSize(2000))
                        .build();
        HttpSpan httpSpan =
                HttpSpan.builder()
                        .info(
                                HttpSpan.Info.builder()
                                        .httpInfo(
                                                HttpSpan.HttpInfo.builder()
                                                        .request(request)
                                                        .response(response)
                                                        .build())
                                        .build())
                        .build();

        httpSpan = (HttpSpan) SpansContainer.getInstance().reduceSpanSize(httpSpan, true);

        Assert.assertEquals(
                "Wrong reduce size",
                2000,
                httpSpan.getInfo().getHttpInfo().getRequest().getHeaders().length());
        Assert.assertEquals(
                "Wrong reduce size",
                2000,
                httpSpan.getInfo().getHttpInfo().getRequest().getBody().length());
        Assert.assertEquals(
                "Wrong reduce size",
                2000,
                httpSpan.getInfo().getHttpInfo().getResponse().getBody().length());
        Assert.assertEquals(
                "Wrong reduce size",
                2000,
                httpSpan.getInfo().getHttpInfo().getResponse().getBody().length());
    }

    private Map<String, String> createMockedEnv() {
        Map<String, String> env = new HashMap<>();
        addEnvMock(env, "AWS_EXECUTION_ENV", "JAVA8");
        addEnvMock(env, "AWS_REGION", "us-west-2");
        addEnvMock(env, "_X_AMZN_TRACE_ID", "Root=1-2-3;Another=456;Bla=789");
        when(envUtil.getEnv()).thenReturn(env);
        when(envUtil.getIntegerEnv(any(), any())).thenCallRealMethod();
        return env;
    }

    private void addEnvMock(Map<String, String> env, String key, String value) {
        env.put(key, value);
        when(envUtil.getEnv(key)).thenReturn(value);
    }

    private void mockContext() {
        when(context.getInvokedFunctionArn())
                .thenReturn("arn:aws:lambda:us-west-2:1111:function:mocked_function_name");
        when(context.getRemainingTimeInMillis()).thenReturn(100);
        when(context.getAwsRequestId()).thenReturn("3n2783hf7823hdui32");
        when(context.getFunctionName()).thenReturn("mocked_function_name");
        when(context.getMemoryLimitInMB()).thenReturn(100);
        when(context.getFunctionVersion()).thenReturn("1.5");
        when(context.getLogGroupName()).thenReturn("/aws/lambda/mocked_function_name");
        when(context.getLogStreamName())
                .thenReturn("2019/05/12/[$LATEST]7f67fc1238a941749d8126be19f0cdc6");
    }

    private static class HttpRequestMockUnResetStream extends HttpRequestMockCls {
        @Override
        public HttpEntity getEntity() {
            return new HttpEntity() {
                @Override
                public boolean isRepeatable() {
                    return false;
                }

                @Override
                public boolean isChunked() {
                    return false;
                }

                @Override
                public long getContentLength() {
                    return 0;
                }

                @Override
                public Header getContentType() {
                    return null;
                }

                @Override
                public Header getContentEncoding() {
                    return null;
                }

                @Override
                public InputStream getContent() throws IOException, UnsupportedOperationException {
                    return new StringInputStream("awsResponse") {
                        @Override
                        public boolean markSupported() {
                            return false;
                        }
                    };
                }

                @Override
                public void writeTo(OutputStream outputStream) throws IOException {}

                @Override
                public boolean isStreaming() {
                    return false;
                }

                @Override
                public void consumeContent() throws IOException {}
            };
        }
    }

    private static class HttpRequestMockCls extends HttpEntityEnclosingRequestBase
            implements HttpUriRequest {

        @Override
        public String getMethod() {
            return "GET";
        }

        @Override
        public HttpEntity getEntity() {
            return new HttpEntity() {
                @Override
                public boolean isRepeatable() {
                    return false;
                }

                @Override
                public boolean isChunked() {
                    return false;
                }

                @Override
                public long getContentLength() {
                    return 0;
                }

                @Override
                public Header getContentType() {
                    return null;
                }

                @Override
                public Header getContentEncoding() {
                    return null;
                }

                @Override
                public InputStream getContent() throws IOException, UnsupportedOperationException {
                    return new StringInputStream("awsResponse");
                }

                @Override
                public void writeTo(OutputStream outputStream) throws IOException {}

                @Override
                public boolean isStreaming() {
                    return false;
                }

                @Override
                public void consumeContent() throws IOException {}
            };
        }
    }

    private String createStringOfSize(int size) {
        char[] charArray = new char[size];
        Arrays.fill(new char[size], ch);
        return new String(charArray);
    }
}
