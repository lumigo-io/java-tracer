package io.lumigo.core.instrumentation.agent;

import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.not;

import io.lumigo.core.instrumentation.impl.*;
import net.bytebuddy.agent.builder.AgentBuilder;
import org.pmw.tinylog.Logger;

import java.util.Arrays;

@SuppressWarnings("unused")
public class Loader {
    public static void instrument(java.lang.instrument.Instrumentation inst) {
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
//                        .ignore(
//                                not(nameStartsWith("com.amazonaws.http.AmazonHttpClient"))
//                                        .and(not(nameStartsWith("org.apache.http.impl.client")))
//                                        .and(
//                                                not(
//                                                        nameStartsWith(
//                                                                AmazonHttpClientV2Instrumentation
//                                                                        .INSTRUMENTATION_PACKAGE_PREFIX)))
//                                        .and(
//                                                not(
//                                                        nameStartsWith(
//                                                                ApacheKafkaProducerInstrumentation
//                                                                        .INSTRUMENTATION_PACKAGE_PREFIX)))
//                                        .and(
//                                                not(
//                                                        nameStartsWith(
//                                                                ApacheKafkaConsumerInstrumentation
//                                                                        .INSTRUMENTATION_PACKAGE_PREFIX)))
//                                        .and(
//                                                not(
//                                                        nameStartsWith(
//                                                                AwsLambdaRequestHandlerInstrumentation
//                                                                        .INSTRUMENTATION_PACKAGE_PREFIX)))
//                        )
                        .type(apacheHttpInstrumentation.getTypeMatcher())
                        .transform(apacheHttpInstrumentation.getTransformer())
                        .type(amazonHttpClientInstrumentation.getTypeMatcher())
                        .transform(amazonHttpClientInstrumentation.getTransformer())
                        .type(amazonHttpClientV2Instrumentation.getTypeMatcher())
                        .transform(amazonHttpClientV2Instrumentation.getTransformer())
                        .type(apacheKafkaInstrumentation.getTypeMatcher())
                        .transform(apacheKafkaInstrumentation.getTransformer())
                        .type(apacheKafkaConsumerInstrumentation.getTypeMatcher())
                        .transform(apacheKafkaConsumerInstrumentation.getTransformer())
                        .type(awsLambdaRequestHandlerInstrumentation.getTypeMatcher())
                        .transform(awsLambdaRequestHandlerInstrumentation.getTransformer());

        builder.installOn(inst);
        Logger.debug("Finish Instrumentation");
    }
}
