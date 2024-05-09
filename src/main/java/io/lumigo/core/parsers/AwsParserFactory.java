package io.lumigo.core.parsers;

public class AwsParserFactory {
    /**
     * @param serviceName - AWS service name
     * @return Relavant parser if exists or Default parser
     */
    public static AwsParser getParser(String serviceName) {
        if (serviceName == null) {
            return new DefaultParser();
        }
        switch (serviceName) {
            case "AmazonSNS":
                return new SnsParser();
            case "AmazonSQS":
                return new SqsParser();
            case "AmazonKinesis":
                return new KinesisParser();
            case "AmazonDynamoDB":
            case "AmazonDynamoDBv2":
            case "DynamoDb":
                return new DynamoDBParser();
            default:
                return new DefaultParser();
        }
    }
}
