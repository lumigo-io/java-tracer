package io.lumigo.core.parsers.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.lumigo.core.utils.JsonUtils;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class EventParserFactoryTest {

    @Mock private APIGatewayProxyRequestEvent apiGwMockevent;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void test_exception() {
        when(apiGwMockevent.getHeaders()).thenThrow(new RuntimeException());

        assertEquals(apiGwMockevent, EventParserFactory.parseEvent(apiGwMockevent));
    }

    @Test
    public void test_default_event() {
        assertEquals("a", EventParserFactory.parseEvent("a"));
    }

    @Test
    public void test_check_api_gw_event() {
        Map<String, String> pathParameters = Maps.newHashMap();
        pathParameters.put("pathParameterKey", "pathParameterValue");
        Map<String, String> queryStringParameters = Maps.newHashMap();
        queryStringParameters.put("queryStringParametersKey", "queryStringParametersValue");
        Map<String, String> stageVariables = Maps.newHashMap();
        stageVariables.put("stageVariablesKey", "stageVariablesValue");
        Map<String, String> headers = Maps.newHashMap();
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("Accept-Encoding", "gzip); deflate, br");
        headers.put("Accept-Language", "he-IL,he;q=0.9,en-US;q=0.8,en;q=0.7");
        headers.put("Authorization", "auth");
        headers.put("CloudFront-Forwarded-Proto", "https");
        headers.put("CloudFront-Is-Desktop-Viewer", "true");
        headers.put("CloudFront-Is-Mobile-Viewer", "false");
        headers.put("CloudFront-Is-SmartTV-Viewer", "false");
        headers.put("CloudFront-Is-Tablet-Viewer", "false");
        headers.put("CloudFront-Viewer-Country", "IL");
        headers.put("content-type", "application/json;charset=UTF-8");
        headers.put("customer_id", "c_1111");
        headers.put("Host", "aaaa.execute-api.us-west-2.amazonaws.com");
        headers.put("origin", "https://aaa.io");
        headers.put("Referer", "https://aaa.io/users");
        headers.put("sec-fetch-dest", "empty");
        headers.put("sec-fetch-mode", "cors");
        headers.put("sec-fetch-site", "cross-site");
        headers.put(
                "User-Agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.163 Safari/537.36");
        headers.put("Via", "2.0 59574f77a7cf2d23d64904db278e5711.cloudfront.net (CloudFront)");
        headers.put("X-Amz-Cf-Id", "J4KbOEUrZCnUQSLsDq1PyYXmfpVy8x634huSeBX0HCbscgH-N2AtVA==");
        headers.put("X-Amzn-Trace-Id", "Root=1-5e9bf868-1c53a38cfe070266db0bfbd9");
        headers.put("X-Forwarded-For", "5.102.206.161, 54.182.243.106");
        headers.put("X-Forwarded-Port", "443");
        headers.put("X-Forwarded-Proto", "https");
        APIGatewayProxyRequestEvent.ProxyRequestContext proxyRequestContext =
                new APIGatewayProxyRequestEvent.ProxyRequestContext();
        Map<String, Object> authorizer = Maps.newHashMap();
        authorizer.put("authorizerKey", "authorizerValue");
        proxyRequestContext.setAuthorizer(authorizer);
        APIGatewayProxyRequestEvent apiGwEvent = new APIGatewayProxyRequestEvent();
        apiGwEvent.setPath("/path");
        apiGwEvent.setResource("/resource");
        apiGwEvent.setBody("body");
        apiGwEvent.setHttpMethod("GET");
        apiGwEvent.setPathParameters(pathParameters);
        apiGwEvent.setQueryStringParameters(queryStringParameters);
        apiGwEvent.setHeaders(headers);
        apiGwEvent.setStageVariables(stageVariables);
        apiGwEvent.setRequestContext(proxyRequestContext);

        Object event = EventParserFactory.parseEvent(apiGwEvent);

        String expectedEventAsJson =
                "{\"path\":\"/path\","
                        + "\"resource\":\"/resource\","
                        + "\"httpMethod\":\"GET\","
                        + "\"queryStringParameters\":{\"queryStringParametersKey\":\"queryStringParametersValue\"},"
                        + "\"pathParameters\":{\"pathParameterKey\":\"pathParameterValue\"},"
                        + "\"body\":\"body\","
                        + "\"authorizer\":{\"authorizerKey\":\"authorizerValue\"},"
                        + "\"headers\":{\"Authorization\":\"auth\",\"origin\":\"https://aaa.io\",\"User-Agent\":\"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.163 Safari/537.36\",\"Referer\":\"https://aaa.io/users\",\"content-type\":\"application/json;charset=UTF-8\",\"Host\":\"aaaa.execute-api.us-west-2.amazonaws.com\",\"customer_id\":\"c_1111\"},"
                        + "\"stageVariables\":{\"stageVariablesKey\":\"stageVariablesValue\"}}";

        assertEquals(expectedEventAsJson, JsonUtils.getObjectAsJsonString(event));
    }

    @Test
    public void test_check_sns_event() {
        Map<String, SNSEvent.MessageAttribute> messageAttributes = new HashMap<>();
        SNSEvent.MessageAttribute messageAttribute = new SNSEvent.MessageAttribute();
        messageAttribute.setType("type");
        messageAttribute.setValue("value");
        messageAttributes.put("key", messageAttribute);
        SNSEvent.SNS sns = new SNSEvent.SNS();
        sns.setMessage("msg");
        sns.setType("type");
        sns.setSignature("signature");
        sns.setSignatureVersion("signatureVersion");
        sns.setSubject("subject(");
        sns.setTopicArn("topicArn");
        sns.setMessageId("MessageId");
        sns.setMessageAttributes(messageAttributes);
        SNSEvent snsEvent = new SNSEvent();
        SNSEvent.SNSRecord record = new SNSEvent.SNSRecord();
        record.setEventSource("aws:sns");
        record.setSns(sns);
        snsEvent.setRecords(Lists.newArrayList(record));

        Object actualEvent = EventParserFactory.parseEvent(snsEvent);

        String expectedEventAsJson =
                "{\"records\":[{\"message\":\"msg\",\"messageAttributes\":{\"key\":{\"type\":\"type\",\"value\":\"value\"}},\"messageId\":\"MessageId\"}]}";
        assertEquals(expectedEventAsJson, JsonUtils.getObjectAsJsonString(actualEvent));
    }

    @Test
    public void test_check_sqs_event() {
        Map<String, SQSEvent.MessageAttribute> messageAttributes = new HashMap<>();
        SQSEvent.MessageAttribute messageAttribute = new SQSEvent.MessageAttribute();
        messageAttribute.setDataType("type");
        messageAttribute.setStringValue("value");
        messageAttributes.put("key", messageAttribute);
        SQSEvent.SQSMessage record = new SQSEvent.SQSMessage();
        record.setEventSource("aws:sns");
        record.setBody("body");
        record.setMessageAttributes(messageAttributes);
        record.setMessageId("MessageId");
        record.setAwsRegion("region");
        record.setMd5OfBody("Md5OfBody");
        record.setEventSourceArn("arn");
        SQSEvent sqsEvent = new SQSEvent();
        sqsEvent.setRecords(Lists.newArrayList(record));

        Object actualEvent = EventParserFactory.parseEvent(sqsEvent);

        String expectedEventAsJson =
                "{\"records\":[{\"body\":\"body\",\"messageAttributes\":{\"key\":{\"type\":\"type\",\"value\":\"value\"}},\"messageId\":\"MessageId\"}]}";
        assertEquals(expectedEventAsJson, JsonUtils.getObjectAsJsonString(actualEvent));
    }
}
