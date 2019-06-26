package io.lumigo.core.parsers;

public class AwsParserFactory {
    public static IAwsParser getParser(String serviceName) {
        if (serviceName == null) {
            return new DefaultParser();
        }
        switch (serviceName) {
            case "AmazonSNS":
                return new SnsParser();
            default:
                return new DefaultParser();
        }
    }
}
