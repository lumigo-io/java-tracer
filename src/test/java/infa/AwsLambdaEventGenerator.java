package infa;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.events.*;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.s3.*;
import com.sun.tools.javac.util.List;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AwsLambdaEventGenerator {
    public S3Event s3Event() {
        S3Event s3Event = mock(S3Event.class);
        S3EventNotification.S3Entity s3Entity = mock(S3EventNotification.S3Entity.class);
        S3EventNotification.S3EventNotificationRecord s3EventNotificationRecord =
                mock(S3EventNotification.S3EventNotificationRecord.class);
        S3EventNotification.S3BucketEntity bucketEntity =
                mock(S3EventNotification.S3BucketEntity.class);
        when(s3EventNotificationRecord.getS3()).thenReturn(s3Entity);
        when(s3EventNotificationRecord.getS3().getBucket()).thenReturn(bucketEntity);
        when(s3EventNotificationRecord.getS3().getBucket().getArn()).thenReturn("s3-arn");
        when(s3Event.getRecords()).thenReturn(Collections.singletonList(s3EventNotificationRecord));
        return s3Event;
    }

    public DynamodbEvent dynamodbUnknownEvent() {
        DynamodbEvent dynamodbEvent = mock(DynamodbEvent.class);
        DynamodbEvent.DynamodbStreamRecord record = mock(DynamodbEvent.DynamodbStreamRecord.class);
        when(dynamodbEvent.getRecords()).thenReturn(List.of(record));
        when(record.getEventSourceARN()).thenReturn("dynamodb-arn");
        when(record.getEventName()).thenReturn("UNKNOWN");
        return dynamodbEvent;
    }

    public DynamodbEvent dynamodbPartialEvent() {
        DynamodbEvent dynamodbEvent = mock(DynamodbEvent.class);
        DynamodbEvent.DynamodbStreamRecord record = mock(DynamodbEvent.DynamodbStreamRecord.class);
        when(dynamodbEvent.getRecords()).thenReturn(Collections.singletonList(record));
        when(record.getEventSourceARN()).thenReturn("dynamodb-arn");
        return dynamodbEvent;
    }

    public DynamodbEvent dynamodbEvent() {
        DynamodbEvent dynamodbEvent = mock(DynamodbEvent.class);
        DynamodbEvent.DynamodbStreamRecord record = mock(DynamodbEvent.DynamodbStreamRecord.class);
        StreamRecord streamRecord = mock(StreamRecord.class);
        DynamodbEvent.DynamodbStreamRecord record2 = mock(DynamodbEvent.DynamodbStreamRecord.class);
        StreamRecord streamRecord2 = mock(StreamRecord.class);
        when(dynamodbEvent.getRecords()).thenReturn(List.of(record, record2));
        when(record.getEventSourceARN()).thenReturn("dynamodb-arn");
        when(record.getEventName()).thenReturn("INSERT");
        when(record.getDynamodb()).thenReturn(streamRecord);
        when(streamRecord.getApproximateCreationDateTime()).thenReturn(new Date(769554000));
        when(streamRecord.getNewImage())
                .thenReturn(Collections.singletonMap("k", new AttributeValue("v")));
        when(record2.getEventName()).thenReturn("MODIFY");
        when(record2.getDynamodb()).thenReturn(streamRecord2);
        when(streamRecord2.getKeys())
                .thenReturn(Collections.singletonMap("k2", new AttributeValue("v2")));
        return dynamodbEvent;
    }

    public KinesisEvent kinesisEvent() {
        KinesisEvent kinesisEvent = mock(KinesisEvent.class);
        KinesisEvent.KinesisEventRecord record = mock(KinesisEvent.KinesisEventRecord.class);
        KinesisEvent.Record kinesisRecord = mock(KinesisEvent.Record.class);
        when(kinesisEvent.getRecords()).thenReturn(Collections.singletonList(record));
        when(record.getEventSourceARN()).thenReturn("kinesis-arn");
        when(kinesisRecord.getSequenceNumber()).thenReturn("1");
        when(record.getKinesis()).thenReturn(kinesisRecord);
        return kinesisEvent;
    }

    public KinesisFirehoseEvent kinesisFirehoseEvent() {
        KinesisFirehoseEvent kinesisFirehoseEvent = mock(KinesisFirehoseEvent.class);
        when(kinesisFirehoseEvent.getDeliveryStreamArn()).thenReturn("kinesis-arn");
        return kinesisFirehoseEvent;
    }

    public KinesisAnalyticsFirehoseInputPreprocessingEvent
            kinesisAnalyticsFirehoseInputPreprocessingEvent() {
        KinesisAnalyticsFirehoseInputPreprocessingEvent kinesisFirehoseEvent =
                mock(KinesisAnalyticsFirehoseInputPreprocessingEvent.class);
        when(kinesisFirehoseEvent.getStreamArn()).thenReturn("kinesis-arn");
        return kinesisFirehoseEvent;
    }

    public KinesisAnalyticsStreamsInputPreprocessingEvent
            kinesisAnalyticsStreamsInputPreprocessingEvent() {
        KinesisAnalyticsStreamsInputPreprocessingEvent kinesisFirehoseEvent =
                mock(KinesisAnalyticsStreamsInputPreprocessingEvent.class);
        when(kinesisFirehoseEvent.getStreamArn()).thenReturn("kinesis-arn");
        return kinesisFirehoseEvent;
    }

    public SNSEvent snsEvent() {
        SNSEvent snsEvent = mock(SNSEvent.class);
        SNSEvent.SNSRecord record = mock(SNSEvent.SNSRecord.class);
        SNSEvent.SNS sns = mock(SNSEvent.SNS.class);
        when(snsEvent.getRecords()).thenReturn(Collections.singletonList(record));
        when(record.getSNS()).thenReturn(sns);
        when(record.getSNS().getTopicArn()).thenReturn("sns-arn");
        return snsEvent;
    }

    public SQSEvent sqsEvent() {
        SQSEvent sqsEvent = mock(SQSEvent.class);
        SQSEvent.SQSMessage record = mock(SQSEvent.SQSMessage.class);
        when(sqsEvent.getRecords()).thenReturn(Collections.singletonList(record));
        when(record.getEventSourceArn()).thenReturn("sqs-arn");
        when(record.getMessageId()).thenReturn("sqs-message-id");
        return sqsEvent;
    }

    public APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent() {
        APIGatewayProxyRequestEvent apigw = mock(APIGatewayProxyRequestEvent.class);
        APIGatewayProxyRequestEvent.ProxyRequestContext context =
                mock(APIGatewayProxyRequestEvent.ProxyRequestContext.class);
        Map<String, String> headers = new HashMap<>();
        headers.put("Host", "www.host.com");
        when(apigw.getHttpMethod()).thenReturn("method");
        when(apigw.getResource()).thenReturn("resource");
        when(apigw.getRequestContext()).thenReturn(context);
        when(context.getStage()).thenReturn("stage");
        when(apigw.getHeaders()).thenReturn(headers);
        return apigw;
    }

    public CloudWatchLogsEvent cloudWatchLogsEvent() {
        return mock(CloudWatchLogsEvent.class);
    }

    public ScheduledEvent scheduledEvent() {
        ScheduledEvent event = mock(ScheduledEvent.class);
        when(event.getResources()).thenReturn(Collections.singletonList("arn"));
        return event;
    }

    public CloudFrontEvent cloudFrontEvent() {
        return mock(CloudFrontEvent.class);
    }

    public CodeCommitEvent codeCommitEvent() {
        return mock(CodeCommitEvent.class);
    }

    public LexEvent lexEvent() {
        return mock(LexEvent.class);
    }

    public CognitoEvent cognitoEvent() {
        return mock(CognitoEvent.class);
    }

    public KafkaEvent kafkaEvent() {
        return new KafkaEvent(
                Collections.singletonMap(
                        "msk-topic-0",
                        Collections.singletonList(
                                KafkaEvent.KafkaEventRecord.builder()
                                        .withTopic("msk-topic")
                                        .withPartition(0)
                                        .withOffset(1612)
                                        .withTimestamp(1716147063028L)
                                        .withTimestampType("CREATE_TIME")
                                        .withKey("a2V5")
                                        .withValue("TWVzc2FnZSBhdCAxNzE2MTQ3MDYzMDE1")
                                        .withHeaders(
                                                Collections.singletonList(
                                                        Collections.singletonMap(
                                                                "lumigoMessageId",
                                                                "NjA1ZTU0YWItMA==".getBytes())))
                                        .build())),
                "aws:kafka",
                "arn:aws:kafka:us-west-2:779055952581:cluster/MskLambdaStackCluster/8fff24de-4f6c-44ca-b072-61d7a1b450a4-2",
                "b-2.msklambdastackcluster.29ysmg.c2.kafka.us-west-2.amazonaws.com:9098,b-1.msklambdastackcluster.29ysmg.c2.kafka.us-west-2.amazonaws.com:9098");
    }
}
