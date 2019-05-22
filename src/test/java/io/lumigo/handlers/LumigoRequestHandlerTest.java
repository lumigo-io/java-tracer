package io.lumigo.handlers;

import static io.lumigo.core.utils.AwsUtils.COLD_START_KEY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.lumigo.core.SpansContainer;
import io.lumigo.core.configuration.Configuration;
import io.lumigo.core.network.Reporter;
import io.lumigo.core.utils.EnvUtil;
import io.lumigo.core.utils.JsonUtils;
import io.lumigo.models.Span;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

@RunWith(MockitoJUnitRunner.class)
class LumigoRequestHandlerTest {

    static class Handler extends LumigoRequestHandler<KinesisEvent, String> {
        @Override
        public String doHandleRequest(KinesisEvent kinesisEvent, Context context) {
            return "Response";
        }
    }

    static class HandlerWithException extends LumigoRequestHandler<KinesisEvent, String> {
        @Override
        public String doHandleRequest(KinesisEvent kinesisEvent, Context context) {
            throw new UnsupportedOperationException();
        }
    }

    static class HandlerStaticInit extends LumigoRequestHandler<KinesisEvent, String> {

        static {
            LumigoConfiguration.builder().token("123456789").verbose(false).build().init();
        }

        @Override
        public String doHandleRequest(KinesisEvent kinesisEvent, Context context) {
            return "Response";
        }
    }

    static class HandlerStream extends LumigoRequestStreamHandler {

        @Override
        public void doHandleRequest(
                InputStream inputStream, OutputStream outputStream, Context context)
                throws IOException {}
    }

    static class HandlerStreamWithException extends LumigoRequestStreamHandler {
        @Override
        public void doHandleRequest(
                InputStream inputStream, OutputStream outputStream, Context context) {
            throw new UnsupportedOperationException();
        }
    }

    static class HandlerStreamStaticInit extends LumigoRequestStreamHandler {

        static {
            LumigoConfiguration.builder().token("123456789").verbose(false).build().init();
        }

        @Override
        public void doHandleRequest(
                InputStream inputStream, OutputStream outputStream, Context context)
                throws IOException {}
    }

    @Mock Context context;
    @Mock EnvUtil envUtil;
    @Mock Reporter reporter;
    private Map<String, String> env = new HashMap<>();
    private KinesisEvent kinesisEvent;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        mockContext();
        createMockedEnv();

        KinesisEvent.KinesisEventRecord record = new KinesisEvent.KinesisEventRecord();
        record.setEventSourceARN("arn");
        kinesisEvent = new KinesisEvent();
        kinesisEvent.setRecords(Collections.singletonList(record));

        LumigoConfiguration.builder().build().init();
        System.clearProperty(COLD_START_KEY);
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

    private void createMockedEnv() {
        addEnvMock("_X_AMZN_TRACE_ID", "Root=1-2-3;Another=456;Bla=789");
        addEnvMock("AWS_REGION", "us-west-2");
        addEnvMock("AWS_EXECUTION_ENV", "JAVA8");
        addEnvMock(Configuration.TOKEN_KEY, "test-token");
        when(envUtil.getEnv()).thenReturn(env);
    }

    private void addEnvMock(String key, String value) {
        env.put(key, value);
        when(envUtil.getEnv(key)).thenReturn(value);
    }

    /**
     * *************************************
     *
     * <p>LumigoRequestHandler tests
     *
     * <p>************************************
     */
    @Test
    public void LumigoRequestHandler_happy_flow_response() throws Exception {
        Handler handler = new Handler();
        handler.setEnvUtil(envUtil);
        handler.setReporter(reporter);
        Configuration.getInstance().setEnvUtil(envUtil);

        String response = handler.handleRequest(kinesisEvent, context);

        ArgumentCaptor<List> argumentCaptorAllSpans = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Span> argumentCaptorStartSpan = ArgumentCaptor.forClass(Span.class);
        verify(reporter, Mockito.times(1)).reportSpans(argumentCaptorAllSpans.capture());
        verify(reporter, Mockito.times(1)).reportSpans(argumentCaptorStartSpan.capture());

        assertEquals("Response", response);
        JSONAssert.assertEquals(
                JsonUtils.getObjectAsJsonString(getStartSpan(true)),
                JsonUtils.getObjectAsJsonString(argumentCaptorStartSpan.getAllValues().get(0)),
                new CustomComparator(
                        JSONCompareMode.LENIENT,
                        new Customization("started", (o1, o2) -> o2 != null),
                        new Customization("ended", (o1, o2) -> o2 != null)));
        JSONAssert.assertEquals(
                JsonUtils.getObjectAsJsonString(getEndSpan("Response", null)),
                JsonUtils.getObjectAsJsonString(
                        argumentCaptorAllSpans.getAllValues().get(0).get(0)),
                new CustomComparator(
                        JSONCompareMode.LENIENT,
                        new Customization("started", (o1, o2) -> o2 != null),
                        new Customization("ended", (o1, o2) -> o2 != null)));
    }

    @Test
    public void LumigoRequestHandler_happy_flow_error() throws Exception {
        HandlerWithException handler = new HandlerWithException();
        handler.setEnvUtil(envUtil);
        handler.setReporter(reporter);
        Configuration.getInstance().setEnvUtil(envUtil);

        assertThrows(
                UnsupportedOperationException.class,
                () -> handler.handleRequest(kinesisEvent, context));

        ArgumentCaptor<List> argumentCaptorAllSpans = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Span> argumentCaptorStartSpan = ArgumentCaptor.forClass(Span.class);
        verify(reporter, Mockito.times(1)).reportSpans(argumentCaptorAllSpans.capture());
        verify(reporter, Mockito.times(1)).reportSpans(argumentCaptorStartSpan.capture());

        JSONAssert.assertEquals(
                JsonUtils.getObjectAsJsonString(getStartSpan(true)),
                JsonUtils.getObjectAsJsonString(argumentCaptorStartSpan.getAllValues().get(0)),
                new CustomComparator(
                        JSONCompareMode.LENIENT,
                        new Customization("started", (o1, o2) -> o2 != null),
                        new Customization("ended", (o1, o2) -> o2 != null)));
        JSONAssert.assertEquals(
                JsonUtils.getObjectAsJsonString(
                        getEndSpan(null, new UnsupportedOperationException())),
                JsonUtils.getObjectAsJsonString(
                        argumentCaptorAllSpans.getAllValues().get(0).get(0)),
                new CustomComparator(
                        JSONCompareMode.LENIENT,
                        new Customization("started", (o1, o2) -> o2 != null),
                        new Customization("error.stacktrace", (o1, o2) -> o2 != null),
                        new Customization("ended", (o1, o2) -> o2 != null)));
    }

    @Test
    public void LumigoRequestHandler_happy_with_inline_configuration() throws Exception {
        HandlerStaticInit handler = new HandlerStaticInit();
        handler.setEnvUtil(envUtil);
        handler.setReporter(reporter);
        Configuration.getInstance().setEnvUtil(envUtil);

        handler.handleRequest(kinesisEvent, context);

        ArgumentCaptor<List> argumentCaptorAllSpans = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Span> argumentCaptorStartSpan = ArgumentCaptor.forClass(Span.class);
        verify(reporter, Mockito.times(1)).reportSpans(argumentCaptorAllSpans.capture());
        verify(reporter, Mockito.times(1)).reportSpans(argumentCaptorStartSpan.capture());

        JSONAssert.assertEquals(
                JsonUtils.getObjectAsJsonString(
                        getStartSpan(true)
                                .toBuilder()
                                .token("123456789")
                                .envs(null)
                                .event(null)
                                .build()),
                JsonUtils.getObjectAsJsonString(argumentCaptorStartSpan.getAllValues().get(0)),
                new CustomComparator(
                        JSONCompareMode.LENIENT,
                        new Customization("started", (o1, o2) -> o2 != null),
                        new Customization("ended", (o1, o2) -> o2 != null)));
        JSONAssert.assertEquals(
                JsonUtils.getObjectAsJsonString(
                        getEndSpan(null, null)
                                .toBuilder()
                                .token("123456789")
                                .envs(null)
                                .event(null)
                                .return_value(null)
                                .build()),
                JsonUtils.getObjectAsJsonString(
                        argumentCaptorAllSpans.getAllValues().get(0).get(0)),
                new CustomComparator(
                        JSONCompareMode.LENIENT,
                        new Customization("started", (o1, o2) -> o2 != null),
                        new Customization("error.stacktrace", (o1, o2) -> o2 != null),
                        new Customization("ended", (o1, o2) -> o2 != null)));
    }

    @Test
    public void LumigoRequestHandler_internal_exception() throws Exception {
        Handler handler = new Handler();
        SpansContainer spansContainerMock = Mockito.mock(SpansContainer.class);
        doThrow(new RuntimeException()).when(spansContainerMock).start();
        doThrow(new RuntimeException()).when(spansContainerMock).getStartFunctionSpan();
        doThrow(new RuntimeException()).when(spansContainerMock).end(any());
        doThrow(new RuntimeException()).when(spansContainerMock).getAllCollectedSpans();
        handler.setReporter(reporter);
        handler.setSpansContainer(spansContainerMock);
        Configuration.getInstance().setEnvUtil(envUtil);

        String response = handler.handleRequest(kinesisEvent, context);

        ArgumentCaptor<List> argumentCaptorAllSpans = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Span> argumentCaptorStartSpan = ArgumentCaptor.forClass(Span.class);
        verify(reporter, Mockito.times(0)).reportSpans(argumentCaptorAllSpans.capture());
        verify(reporter, Mockito.times(0)).reportSpans(argumentCaptorStartSpan.capture());
        assertEquals("Response", response);
    }

    @Test
    public void LumigoRequestHandler_internal_exception_with_lambda_exception() throws Exception {
        HandlerWithException handler = new HandlerWithException();
        SpansContainer spansContainerMock = Mockito.mock(SpansContainer.class);
        doThrow(new RuntimeException()).when(spansContainerMock).start();
        doThrow(new RuntimeException()).when(spansContainerMock).getStartFunctionSpan();
        doThrow(new RuntimeException()).when(spansContainerMock).endWithException(any());
        doThrow(new RuntimeException()).when(spansContainerMock).getAllCollectedSpans();
        handler.setReporter(reporter);
        handler.setSpansContainer(spansContainerMock);
        Configuration.getInstance().setEnvUtil(envUtil);

        assertThrows(
                UnsupportedOperationException.class,
                () -> handler.handleRequest(kinesisEvent, context));

        ArgumentCaptor<List> argumentCaptorAllSpans = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Span> argumentCaptorStartSpan = ArgumentCaptor.forClass(Span.class);
        verify(reporter, Mockito.times(0)).reportSpans(argumentCaptorAllSpans.capture());
        verify(reporter, Mockito.times(0)).reportSpans(argumentCaptorStartSpan.capture());
    }
    /**
     * *************************************
     *
     * <p>LumigoRequestStreamHandler tests
     *
     * <p>************************************
     */
    @Test
    public void LumigoRequestStreamHandler_happy_flow_response() throws Exception {
        HandlerStream handler = new HandlerStream();
        handler.setEnvUtil(envUtil);
        handler.setReporter(reporter);
        Configuration.getInstance().setEnvUtil(envUtil);

        handler.handleRequest(null, null, context);

        ArgumentCaptor<List> argumentCaptorAllSpans = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Span> argumentCaptorStartSpan = ArgumentCaptor.forClass(Span.class);
        verify(reporter, Mockito.times(1)).reportSpans(argumentCaptorAllSpans.capture());
        verify(reporter, Mockito.times(1)).reportSpans(argumentCaptorStartSpan.capture());

        JSONAssert.assertEquals(
                JsonUtils.getObjectAsJsonString(
                        getStartSpan(false).toBuilder().event(null).build()),
                JsonUtils.getObjectAsJsonString(argumentCaptorStartSpan.getAllValues().get(0)),
                new CustomComparator(
                        JSONCompareMode.LENIENT,
                        new Customization("started", (o1, o2) -> o2 != null),
                        new Customization("ended", (o1, o2) -> o2 != null)));
        JSONAssert.assertEquals(
                JsonUtils.getObjectAsJsonString(
                        getEndSpan(null, null, false).toBuilder().event(null).build()),
                JsonUtils.getObjectAsJsonString(
                        argumentCaptorAllSpans.getAllValues().get(0).get(0)),
                new CustomComparator(
                        JSONCompareMode.LENIENT,
                        new Customization("started", (o1, o2) -> o2 != null),
                        new Customization("ended", (o1, o2) -> o2 != null)));
    }

    @Test
    public void LumigoRequestStreamHandler_happy_flow_error() throws Exception {
        HandlerStreamWithException handler = new HandlerStreamWithException();
        handler.setEnvUtil(envUtil);
        handler.setReporter(reporter);
        Configuration.getInstance().setEnvUtil(envUtil);

        assertThrows(
                UnsupportedOperationException.class,
                () -> handler.handleRequest(null, null, context));

        ArgumentCaptor<List> argumentCaptorAllSpans = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Span> argumentCaptorStartSpan = ArgumentCaptor.forClass(Span.class);
        verify(reporter, Mockito.times(1)).reportSpans(argumentCaptorAllSpans.capture());
        verify(reporter, Mockito.times(1)).reportSpans(argumentCaptorStartSpan.capture());

        JSONAssert.assertEquals(
                JsonUtils.getObjectAsJsonString(
                        getStartSpan(false).toBuilder().event(null).build()),
                JsonUtils.getObjectAsJsonString(argumentCaptorStartSpan.getAllValues().get(0)),
                new CustomComparator(
                        JSONCompareMode.LENIENT,
                        new Customization("started", (o1, o2) -> o2 != null),
                        new Customization("ended", (o1, o2) -> o2 != null)));
        JSONAssert.assertEquals(
                JsonUtils.getObjectAsJsonString(
                        getEndSpan(null, new UnsupportedOperationException(), false)
                                .toBuilder()
                                .event(null)
                                .build()),
                JsonUtils.getObjectAsJsonString(
                        argumentCaptorAllSpans.getAllValues().get(0).get(0)),
                new CustomComparator(
                        JSONCompareMode.LENIENT,
                        new Customization("started", (o1, o2) -> o2 != null),
                        new Customization("error.stacktrace", (o1, o2) -> o2 != null),
                        new Customization("ended", (o1, o2) -> o2 != null)));
    }

    @Test
    public void LumigoRequestStreamHandler_happy_with_inline_configuration() throws Exception {
        HandlerStreamStaticInit handler = new HandlerStreamStaticInit();
        handler.setEnvUtil(envUtil);
        handler.setReporter(reporter);
        Configuration.getInstance().setEnvUtil(envUtil);

        handler.handleRequest(null, null, context);

        ArgumentCaptor<List> argumentCaptorAllSpans = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Span> argumentCaptorStartSpan = ArgumentCaptor.forClass(Span.class);
        verify(reporter, Mockito.times(1)).reportSpans(argumentCaptorAllSpans.capture());
        verify(reporter, Mockito.times(1)).reportSpans(argumentCaptorStartSpan.capture());

        JSONAssert.assertEquals(
                JsonUtils.getObjectAsJsonString(
                        getStartSpan(false)
                                .toBuilder()
                                .token("123456789")
                                .envs(null)
                                .event(null)
                                .build()),
                JsonUtils.getObjectAsJsonString(argumentCaptorStartSpan.getAllValues().get(0)),
                new CustomComparator(
                        JSONCompareMode.LENIENT,
                        new Customization("started", (o1, o2) -> o2 != null),
                        new Customization("ended", (o1, o2) -> o2 != null)));
        JSONAssert.assertEquals(
                JsonUtils.getObjectAsJsonString(
                        getEndSpan(null, null, false)
                                .toBuilder()
                                .token("123456789")
                                .envs(null)
                                .event(null)
                                .return_value(null)
                                .build()),
                JsonUtils.getObjectAsJsonString(
                        argumentCaptorAllSpans.getAllValues().get(0).get(0)),
                new CustomComparator(
                        JSONCompareMode.LENIENT,
                        new Customization("started", (o1, o2) -> o2 != null),
                        new Customization("error.stacktrace", (o1, o2) -> o2 != null),
                        new Customization("ended", (o1, o2) -> o2 != null)));
    }

    @Test
    public void LumigoRequestStreamHandler_internal_exception() throws Exception {
        HandlerStream handler = new HandlerStream();
        SpansContainer spansContainerMock = Mockito.mock(SpansContainer.class);
        doThrow(new RuntimeException()).when(spansContainerMock).start();
        doThrow(new RuntimeException()).when(spansContainerMock).getStartFunctionSpan();
        doThrow(new RuntimeException()).when(spansContainerMock).end();
        doThrow(new RuntimeException()).when(spansContainerMock).getAllCollectedSpans();
        handler.setReporter(reporter);
        handler.setSpansContainer(spansContainerMock);
        Configuration.getInstance().setEnvUtil(envUtil);

        handler.handleRequest(null, null, context);

        ArgumentCaptor<List> argumentCaptorAllSpans = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Span> argumentCaptorStartSpan = ArgumentCaptor.forClass(Span.class);
        verify(reporter, Mockito.times(0)).reportSpans(argumentCaptorAllSpans.capture());
        verify(reporter, Mockito.times(0)).reportSpans(argumentCaptorStartSpan.capture());
    }

    @Test
    public void LumigoRequestStreamHandler_internal_exception_with_lambda_exception()
            throws Exception {
        HandlerStreamWithException handler = new HandlerStreamWithException();
        SpansContainer spansContainerMock = Mockito.mock(SpansContainer.class);
        doThrow(new RuntimeException()).when(spansContainerMock).start();
        doThrow(new RuntimeException()).when(spansContainerMock).getStartFunctionSpan();
        doThrow(new RuntimeException()).when(spansContainerMock).endWithException(any());
        doThrow(new RuntimeException()).when(spansContainerMock).getAllCollectedSpans();
        handler.setReporter(reporter);
        handler.setSpansContainer(spansContainerMock);
        Configuration.getInstance().setEnvUtil(envUtil);

        assertThrows(
                UnsupportedOperationException.class,
                () -> handler.handleRequest(null, null, context));

        ArgumentCaptor<List> argumentCaptorAllSpans = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Span> argumentCaptorStartSpan = ArgumentCaptor.forClass(Span.class);
        verify(reporter, Mockito.times(0)).reportSpans(argumentCaptorAllSpans.capture());
        verify(reporter, Mockito.times(0)).reportSpans(argumentCaptorStartSpan.capture());
    }

    private Span getStartSpan(boolean includeRiggeredBy) throws JsonProcessingException {
        return Span.builder()
                .name("mocked_function_name")
                .runtime("JAVA8")
                .id("3n2783hf7823hdui32_started")
                .memoryAllocated(100)
                .type("function")
                .transactionId("3")
                .requestId("3n2783hf7823hdui32")
                .account("1111")
                .maxFinishTime(100)
                .event(JsonUtils.getObjectAsJsonString(kinesisEvent))
                .envs(JsonUtils.getObjectAsJsonString(env))
                .region("us-west-2")
                .token("test-token")
                .info(
                        Span.Info.builder()
                                .tracer(Span.Tracer.builder().version("1.0").build())
                                .traceId(Span.TraceId.builder().root("1-2-3").build())
                                .triggeredBy(
                                        includeRiggeredBy
                                                ? "{\"triggeredBy\":\"kinesis\",\"arn\":\"arn\"}"
                                                : null)
                                .logStreamName(
                                        "2019/05/12/[$LATEST]7f67fc1238a941749d8126be19f0cdc6")
                                .logGroupName("/aws/lambda/mocked_function_name")
                                .build())
                .readiness(Span.READINESS.COLD.name().toLowerCase())
                .build();
    }

    private Span getEndSpan(Object returnValue, Exception e) throws JsonProcessingException {
        return getEndSpan(returnValue, e, true);
    }

    private Span getEndSpan(Object returnValue, Exception e, boolean triggerBy)
            throws JsonProcessingException {
        return Span.builder()
                .name("mocked_function_name")
                .runtime("JAVA8")
                .id("3n2783hf7823hdui32")
                .memoryAllocated(100)
                .type("function")
                .transactionId("3")
                .requestId("3n2783hf7823hdui32")
                .account("1111")
                .maxFinishTime(100)
                .event(JsonUtils.getObjectAsJsonString(kinesisEvent))
                .envs(JsonUtils.getObjectAsJsonString(env))
                .region("us-west-2")
                .token("test-token")
                .info(
                        Span.Info.builder()
                                .tracer(Span.Tracer.builder().version("1.0").build())
                                .traceId(Span.TraceId.builder().root("1-2-3").build())
                                .triggeredBy(
                                        triggerBy
                                                ? "{\"triggeredBy\":\"kinesis\",\"arn\":\"arn\"}"
                                                : null)
                                .logStreamName(
                                        "2019/05/12/[$LATEST]7f67fc1238a941749d8126be19f0cdc6")
                                .logGroupName("/aws/lambda/mocked_function_name")
                                .build())
                .readiness(Span.READINESS.COLD.name().toLowerCase())
                .return_value(
                        returnValue != null ? JsonUtils.getObjectAsJsonString(returnValue) : null)
                .error(
                        e != null
                                ? Span.Error.builder()
                                        .message(e.getMessage())
                                        .type(e.getClass().getName())
                                        .build()
                                : null)
                .build();
    }
}
