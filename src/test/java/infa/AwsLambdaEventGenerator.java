package infa;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.events.*;
import com.amazonaws.services.s3.event.S3EventNotification;
import java.util.Collections;
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

    public DynamodbEvent dynamodbEvent() {
        DynamodbEvent dynamodbEvent = mock(DynamodbEvent.class);
        DynamodbEvent.DynamodbStreamRecord record = mock(DynamodbEvent.DynamodbStreamRecord.class);
        when(dynamodbEvent.getRecords()).thenReturn(Collections.singletonList(record));
        when(record.getEventSourceARN()).thenReturn("dynamodb-arn");
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
        CloudWatchLogsEvent cloudWatchLogsEvent = mock(CloudWatchLogsEvent.class);
        return cloudWatchLogsEvent;
    }

    public ScheduledEvent scheduledEvent() {
        ScheduledEvent event = mock(ScheduledEvent.class);
        when(event.getResources()).thenReturn(Collections.singletonList("arn"));
        return event;
    }

    public CloudFrontEvent cloudFrontEvent() {
        CloudFrontEvent event = mock(CloudFrontEvent.class);
        return event;
    }

    public CodeCommitEvent codeCommitEvent() {
        CodeCommitEvent event = mock(CodeCommitEvent.class);
        return event;
    }

    public LexEvent lexEvent() {
        LexEvent event = mock(LexEvent.class);
        return event;
    }

    public CognitoEvent cognitoEvent() {
        CognitoEvent event = mock(CognitoEvent.class);
        return event;
    }
}
