package io.lumigo.core.parsers.v1;

public class AwsSdkV1ParserFactory {
    /**
     * @param serviceName - AWS service name
     * @return Relavant parser if exists or Default parser
     */
    public static AwsSdkV1Parser getParser(String serviceName) {
        if (serviceName == null) {
            return new DoNothingV1Parser();
        }
        switch (serviceName) {
            case "AmazonSNS":
                return new SnsV1Parser();
            case "AmazonSQS":
                return new SqsV1Parser();
            case "AmazonKinesis":
                return new KinesisV1Parser();
            case "AmazonDynamoDB":
            case "AmazonDynamoDBv2":
                return new DynamoDBV1Parser();
            default:
                return new DoNothingV1Parser();
        }
    }
}
