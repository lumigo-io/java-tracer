package io.lumigo.core.parsers;

public class AwsParserFactory {
    /**
     * @param serviceName - AWS service name
     * @return Relavant parser if exists or Default parser
     */
    public static AwsParser getParser(String serviceName) {
        if (serviceName == null) {
            return new DoNothingParser();
        }
        switch (serviceName) {
            case "AmazonSNS":
            case "SNS":
            case "Sns":
                return new SnsParser();
            case "AmazonSQS":
            case "Sqs":
                return new SqsParser();
            case "AmazonKinesis":
            case "Kinesis":
                return new KinesisParser();
            case "AmazonDynamoDB":
            case "AmazonDynamoDBv2":
            case "DynamoDb":
                return new DynamoDBParser();
            default:
                return new DoNothingParser();
        }
    }
}
