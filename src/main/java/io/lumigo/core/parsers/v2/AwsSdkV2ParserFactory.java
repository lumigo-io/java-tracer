package io.lumigo.core.parsers.v2;

public class AwsSdkV2ParserFactory {
    /**
     * @param serviceName - AWS service name
     * @return Relavant parser if exists or Default parser
     */
    public static AwsSdkV2Parser getParser(String serviceName) {
        if (serviceName == null) {
            return new DoNothingV2Parser();
        }
        switch (serviceName) {
            case "Sns":
                return new SnsV2Parser();
            case "Sqs":
                return new SqsV2Parser();
            case "Kinesis":
                return new KinesisV2Parser();
            case "DynamoDb":
                return new DynamoDBV2Parser();
            default:
                return new DoNothingV2Parser();
        }
    }
}
