package io.lumigo.core.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.http.HttpResponse;
import com.amazonaws.services.kinesis.model.*;
import io.lumigo.models.ContainerHttpSpan;
import java.util.Arrays;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class KinesisParserTest {

    private ContainerHttpSpan span = ContainerHttpSpan.builder().build();
    KinesisParser kinesisParser = new KinesisParser();
    @Mock Request request;
    @Mock HttpResponse httpResponse;
    @Mock PutRecordsRequest putRecordsRequest;
    @Mock PutRecordsResult putRecordsResult;
    @Mock PutRecordRequest putRecordRequest;
    @Mock PutRecordResult putRecordResult;
    Response response;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void test_parse_kinesis_parser_with_no_data() {
        when(request.getParameters()).thenReturn(new HashMap<>());

        kinesisParser.parse(span, request, new Response(null, httpResponse));

        assertNull(span.getResourceName());
        assertNull(span.getTargetArn());
        assertNull(span.getMessageIds());
    }

    @Test
    void test_parse_kinesis_put_record_simple_flow() {
        response = new Response(putRecordResult, httpResponse);
        when(putRecordResult.getSequenceNumber())
                .thenReturn("fee47356-6f6a-58c8-96dc-26d8aaa4631a");
        when(putRecordRequest.getStreamName()).thenReturn("streamName");
        when(request.getOriginalRequest()).thenReturn(putRecordRequest);

        kinesisParser.parse(span, request, response);

        ContainerHttpSpan expectedSpan = ContainerHttpSpan.builder().build();
        expectedSpan.setResourceName("streamName");
        expectedSpan.setMessageIds(Arrays.asList("fee47356-6f6a-58c8-96dc-26d8aaa4631a"));
        assertEquals(span, expectedSpan);
    }

    @Test
    void test_parse_kinesis_put_record_with_exception() {
        response = new Response(putRecordResult, httpResponse);
        when(putRecordResult.getSequenceNumber()).thenThrow(new RuntimeException());
        when(request.getParameters()).thenReturn(new HashMap<>());

        kinesisParser.parse(span, request, response);

        ContainerHttpSpan expectedSpan = ContainerHttpSpan.builder().build();
        assertEquals(span, expectedSpan);
    }

    @Test
    void test_parse_kinesis_put_records_simple_flow() {
        response = new Response(putRecordsResult, httpResponse);
        PutRecordsResultEntry firstResult = new PutRecordsResultEntry();
        PutRecordsResultEntry secResult = new PutRecordsResultEntry();
        firstResult.setSequenceNumber("1");
        secResult.setSequenceNumber("2");
        PutRecordsResultEntry[] results = {firstResult, secResult};
        when(putRecordsResult.getRecords()).thenReturn(Arrays.asList(results));
        when(putRecordsRequest.getStreamName()).thenReturn("streamName");
        when(request.getOriginalRequest()).thenReturn(putRecordsRequest);

        kinesisParser.parse(span, request, response);

        ContainerHttpSpan expectedSpan = ContainerHttpSpan.builder().build();
        expectedSpan.setResourceName("streamName");
        expectedSpan.setMessageIds(Arrays.asList("1", "2"));
        assertEquals(span, expectedSpan);
    }

    @Test
    void test_parse_kinesis_put_records_with_exception_from_request() {
        response = new Response(putRecordsResult, httpResponse);
        when(request.getOriginalRequest()).thenThrow(new RuntimeException());

        kinesisParser.parse(span, request, response);

        ContainerHttpSpan expectedSpan = ContainerHttpSpan.builder().build();
        assertEquals(span, expectedSpan);
    }

    @Test
    void test_parse_kinesis_put_records_with_exception_from_response() {
        response = new Response(putRecordsResult, httpResponse);
        when(putRecordsResult.getRecords()).thenThrow(new RuntimeException());
        when(request.getParameters()).thenReturn(new HashMap<>());

        kinesisParser.parse(span, request, response);

        ContainerHttpSpan expectedSpan = ContainerHttpSpan.builder().build();
        assertEquals(span, expectedSpan);
    }
}
