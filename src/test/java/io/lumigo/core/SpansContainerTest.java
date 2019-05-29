package io.lumigo.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.util.StringInputStream;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.lumigo.core.network.Reporter;
import io.lumigo.core.utils.JsonUtils;
import io.lumigo.models.HttpSpan;
import io.lumigo.models.Span;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

class SpansContainerTest {

    private SpansContainer spansContainer = SpansContainer.getInstance();

    @Mock private Context context;
    @Mock Reporter reporter;
    @Mock HttpResponse httpResponse;
    @Mock StatusLine statusLine;
    @Mock HttpUriRequest httpRequest;

    @BeforeEach
    void setUp() {
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
                        + "  \"memoryAllocated\": 100,\n"
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
        JSONAssert.assertEquals(
                expectedSpan,
                JsonUtils.getObjectAsJsonString(actualSpan),
                new CustomComparator(
                        JSONCompareMode.LENIENT,
                        new Customization("started", (o1, o2) -> o2 != null),
                        new Customization("token", (o1, o2) -> o2 != null),
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
                        + "  \"memoryAllocated\": 100,\n"
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
        JSONAssert.assertEquals(
                expectedSpan,
                JsonUtils.getObjectAsJsonString(actualSpan),
                new CustomComparator(
                        JSONCompareMode.LENIENT,
                        new Customization("started", (o1, o2) -> o2 != null),
                        new Customization("ended", (o1, o2) -> o2 != null),
                        new Customization("token", (o1, o2) -> o2 != null),
                        new Customization("error.stacktrace", (o1, o2) -> o2 != null)));
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
                        + "  \"memoryAllocated\": 100,\n"
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
        JSONAssert.assertEquals(
                expectedSpan,
                JsonUtils.getObjectAsJsonString(actualSpan),
                new CustomComparator(
                        JSONCompareMode.LENIENT,
                        new Customization("started", (o1, o2) -> o2 != null),
                        new Customization("ended", (o1, o2) -> o2 != null),
                        new Customization("token", (o1, o2) -> o2 != null),
                        new Customization("error.stacktrace", (o1, o2) -> o2 != null)));
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
                        + "  \"memoryAllocated\": 100,\n"
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
        JSONAssert.assertEquals(
                expectedSpan,
                JsonUtils.getObjectAsJsonString(actualSpan),
                new CustomComparator(
                        JSONCompareMode.LENIENT,
                        new Customization("started", (o1, o2) -> o2 != null),
                        new Customization("ended", (o1, o2) -> o2 != null),
                        new Customization("token", (o1, o2) -> o2 != null),
                        new Customization("error.stacktrace", (o1, o2) -> o2 != null)));
    }

    @DisplayName("Http span creation")
    @Test
    void add_http_span() throws Exception {
        spansContainer.init(createMockedEnv(), reporter, context, null);
        when(httpRequest.getURI()).thenReturn(URI.create("https://google.com"));
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
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
                        new Customization("id", (o1, o2) -> o2 != null),
                        new Customization("started", (o1, o2) -> o2 != null),
                        new Customization("ended", (o1, o2) -> o2 != null)));
    }

    @DisplayName("Extract body from request")
    @Test
    void test_extract_body_from_request() {
        assertEquals("response", SpansContainer.extractBodyFromRequest(new HttpRequestMockCls()));
    }

    @DisplayName("Extract body from un reset request, should be null")
    @Test
    void test_extract_Body_From_Request() {
        assertNull(SpansContainer.extractBodyFromRequest(new HttpRequestMockUnResetStream()));
    }

    private Map<String, String> createMockedEnv() {
        Map<String, String> env = new HashMap<>();
        env.put("AWS_EXECUTION_ENV", "JAVA8");
        env.put("AWS_REGION", "us-west-2");
        env.put("_X_AMZN_TRACE_ID", "Root=1-2-3;Another=456;Bla=789");
        return env;
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
                    return new StringInputStream("response") {
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
                    return new StringInputStream("response");
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
}
