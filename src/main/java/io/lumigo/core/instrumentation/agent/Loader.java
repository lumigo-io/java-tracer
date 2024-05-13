package io.lumigo.core.instrumentation.agent;

import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.not;

import io.lumigo.core.instrumentation.impl.AmazonHttpClientInstrumentation;
import io.lumigo.core.instrumentation.impl.AmazonHttpClientV2Instrumentation;
import io.lumigo.core.instrumentation.impl.ApacheHttpInstrumentation;
import net.bytebuddy.agent.builder.AgentBuilder;

public class Loader {
    public static void instrument(java.lang.instrument.Instrumentation inst) {
        System.out.println("Start Instrumentation");
        ApacheHttpInstrumentation apacheHttpInstrumentation = new ApacheHttpInstrumentation();
        AmazonHttpClientInstrumentation amazonHttpClientInstrumentation =
                new AmazonHttpClientInstrumentation();
        AmazonHttpClientV2Instrumentation amazonHttpClientV2Instrumentation =
                new AmazonHttpClientV2Instrumentation();
        AgentBuilder builder =
                new AgentBuilder.Default()
                        .disableClassFormatChanges()
                        .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                        .ignore(
                                not(nameStartsWith("com.amazonaws.http.AmazonHttpClient"))
                                        .and(not(nameStartsWith("org.apache.http.impl.client")))
                                        .and(
                                                not(
                                                        nameStartsWith(
                                                                "software.amazon.awssdk.core.client.builder.SdkDefaultClientBuilder"))))
                        .type(apacheHttpInstrumentation.getTypeMatcher())
                        .transform(apacheHttpInstrumentation.getTransformer())
                        .type(amazonHttpClientInstrumentation.getTypeMatcher())
                        .transform(amazonHttpClientInstrumentation.getTransformer())
                        .type(amazonHttpClientV2Instrumentation.getTypeMatcher())
                        .transform(amazonHttpClientV2Instrumentation.getTransformer());

        builder.installOn(inst);
        System.out.println("Finish Instrumentation");
    }
}
