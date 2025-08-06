package io.lumigo.core.instrumentation.impl;

import static net.bytebuddy.matcher.ElementMatchers.*;

import io.lumigo.core.SpansContainer;
import io.lumigo.core.instrumentation.LumigoInstrumentationApi;
import io.lumigo.core.instrumentation.agent.Loader;
import io.lumigo.core.utils.LRUCache;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.internals.ConsumerMetadata;
import org.pmw.tinylog.Logger;

public class ApacheKafkaConsumerInstrumentation implements LumigoInstrumentationApi {
    @Override
    public ElementMatcher<TypeDescription> getTypeMatcher() {
        return named("org.apache.kafka.clients.consumer.KafkaConsumer");
    }

    @Override
    public AgentBuilder.Transformer.ForAdvice getTransformer() {
        return new AgentBuilder.Transformer.ForAdvice()
                .include(Loader.class.getClassLoader())
                .advice(
                        isMethod()
                                .and(isPublic())
                                .and(named("poll"))
                                .and(takesArguments(1))
                                .and(
                                        returns(
                                                named(
                                                        "org.apache.kafka.clients.consumer.ConsumerRecords"))),
                        ApacheKafkaConsumerAdvice.class.getName());
    }

    public static class ApacheKafkaConsumerAdvice {
        public static final SpansContainer spansContainer = SpansContainer.getInstance();
        public static final LRUCache<String, Long> startTimeMap = new LRUCache<>(1000);

        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static void methodEnter(@Advice.FieldValue("clientId") String clientId) {
            try {
                startTimeMap.put(clientId, System.currentTimeMillis());
            } catch (Throwable e) {
                Logger.error(e);
            }
        }

        @Advice.OnMethodExit(suppress = Throwable.class)
        public static void methodExit(
                @Advice.This KafkaConsumer<?, ?> consumer,
                @Advice.FieldValue("metadata") ConsumerMetadata metadata,
                @Advice.FieldValue("clientId") String clientId,
                @Advice.Return ConsumerRecords<?, ?> consumerRecords) {
            try {
                Logger.info("Handling kafka request {}", consumerRecords.hashCode());
                spansContainer.addKafkaConsumeSpan(
                        startTimeMap.get(clientId), consumer, metadata, consumerRecords);
            } catch (Throwable error) {
                Logger.error(error, "Failed to add kafka span");
            }
        }
    }
}
