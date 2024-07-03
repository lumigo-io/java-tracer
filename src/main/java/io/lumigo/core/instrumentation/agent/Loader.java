package io.lumigo.core.instrumentation.agent;

import io.lumigo.core.instrumentation.impl.*;
import net.bytebuddy.agent.builder.AgentBuilder;
import org.pmw.tinylog.Logger;

@SuppressWarnings("unused")
public class Loader {
    public static void instrument(java.lang.instrument.Instrumentation inst, ClassLoader classLoader) {
        Logger.debug("Start Instrumentation");
        ApacheHttpInstrumentation apacheHttpInstrumentation = new ApacheHttpInstrumentation();
        AmazonHttpClientInstrumentation amazonHttpClientInstrumentation =
                new AmazonHttpClientInstrumentation();
        AmazonHttpClientV2Instrumentation amazonHttpClientV2Instrumentation =
                new AmazonHttpClientV2Instrumentation();
        ApacheKafkaProducerInstrumentation apacheKafkaInstrumentation =
                new ApacheKafkaProducerInstrumentation();
        ApacheKafkaConsumerInstrumentation apacheKafkaConsumerInstrumentation =
                new ApacheKafkaConsumerInstrumentation();
        AwsLambdaRequestHandlerInstrumentation awsLambdaRequestHandlerInstrumentation =
                new AwsLambdaRequestHandlerInstrumentation();
        AgentBuilder builder =
                new AgentBuilder.Default()
                        .disableClassFormatChanges()
                        .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                        .type(apacheHttpInstrumentation.getTypeMatcher())
                        .transform(apacheHttpInstrumentation.getTransformer(classLoader))
                        .type(amazonHttpClientInstrumentation.getTypeMatcher())
                        .transform(amazonHttpClientInstrumentation.getTransformer(classLoader))
                        .type(amazonHttpClientV2Instrumentation.getTypeMatcher())
                        .transform(amazonHttpClientV2Instrumentation.getTransformer(classLoader))
                        .type(apacheKafkaInstrumentation.getTypeMatcher())
                        .transform(apacheKafkaInstrumentation.getTransformer(classLoader))
                        .type(apacheKafkaConsumerInstrumentation.getTypeMatcher())
                        .transform(apacheKafkaConsumerInstrumentation.getTransformer(classLoader))
                        .type(awsLambdaRequestHandlerInstrumentation.getTypeMatcher())
                        .transform(awsLambdaRequestHandlerInstrumentation.getTransformer(classLoader))
                        .with(AgentBuilder.Listener.StreamWriting.toSystemError())
                        .with(AgentBuilder.InstallationListener.StreamWriting.toSystemError())
                ;

        builder.installOn(inst);
        Logger.debug("Finish Instrumentation");
    }
}
