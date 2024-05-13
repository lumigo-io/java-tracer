package io.lumigo.core.parsers.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.lumigo.models.HttpSpan;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequestEntry;
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse;
import software.amazon.awssdk.services.kinesis.model.PutRecordsResultEntry;

class KinesisV2ParserTest {

    private HttpSpan span = HttpSpan.builder().info(HttpSpan.Info.builder().build()).build();
    KinesisV2Parser kinesisParser = new KinesisV2Parser();

    @Test
    void test_parse_kinesis_put_record_simple_flow_v2() {
        PutRecordsRequest putRequest =
                PutRecordsRequest.builder()
                        .records(PutRecordsRequestEntry.builder().build())
                        .streamName("streamName")
                        .build();
        PutRecordsResponse putResponse =
                PutRecordsResponse.builder()
                        .records(
                                PutRecordsResultEntry.builder()
                                        .sequenceNumber("fee47356-6f6a-58c8-96dc-26d8aaa4631a")
                                        .build())
                        .build();
        Context.AfterExecution context =
                InterceptorContext.builder().request(putRequest).response(putResponse).build();

        kinesisParser.safeParse(span, context);

        HttpSpan expectedSpan = HttpSpan.builder().info(HttpSpan.Info.builder().build()).build();
        expectedSpan.getInfo().setResourceName("streamName");
        expectedSpan.getInfo().setMessageIds(Arrays.asList("fee47356-6f6a-58c8-96dc-26d8aaa4631a"));
        assertEquals(span, expectedSpan);
    }
}
