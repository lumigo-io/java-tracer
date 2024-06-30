package io.lumigo.core.instrumentation.impl;

import static net.bytebuddy.matcher.ElementMatchers.*;

import io.lumigo.core.SpansContainer;
import io.lumigo.core.instrumentation.LumigoInstrumentationApi;
import io.lumigo.core.instrumentation.agent.Loader;
import io.lumigo.models.KafkaSpan;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.AllArgsConstructor;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.clients.producer.internals.ProducerMetadata;
import org.apache.kafka.common.serialization.Serializer;
import org.pmw.tinylog.Logger;

public class ApacheKafkaProducerInstrumentation implements LumigoInstrumentationApi {
    @Override
    public ElementMatcher<TypeDescription> getTypeMatcher() {
        return named("org.apache.kafka.clients.producer.KafkaProducer");
    }

    @Override
    public AgentBuilder.Transformer.ForAdvice getTransformer() {
        return new AgentBuilder.Transformer.ForAdvice()
                .include(Loader.class.getClassLoader())
                .advice(
                        isMethod()
                                .and(isPublic())
                                .and(named("send"))
                                .and(
                                        takesArgument(
                                                        0,
                                                        named(
                                                                "org.apache.kafka.clients.producer.ProducerRecord"))
                                                .and(
                                                        takesArgument(
                                                                1,
                                                                named(
                                                                        "org.apache.kafka.clients.producer.Callback")))),
                        ApacheKafkaProducerInstrumentation.class.getName() + "$ApacheKafkaProducerAdvice");
    }

    @SuppressWarnings("unused")
    public static class ApacheKafkaProducerAdvice {
        public static final SpansContainer spansContainer = SpansContainer.getInstance();

        @Advice.OnMethodEnter
        public static <K, V> void methodEnter(
                @Advice.FieldValue("metadata") ProducerMetadata metadata,
                @Advice.FieldValue("keySerializer") Serializer<K> keySerializer,
                @Advice.FieldValue("valueSerializer") Serializer<V> valueSerializer,
                @Advice.Argument(value = 0, readOnly = false) ProducerRecord<K, V> record,
                @Advice.Argument(value = 1, readOnly = false) Callback callback) {
            try {
                callback =
                        new KafkaProducerCallback<>(
                                callback,
                                keySerializer,
                                valueSerializer,
                                metadata,
                                record,
                                System.currentTimeMillis());

                // Try to inject correlation id to the kafka record headers
                record.headers()
                        .add(
                                KafkaSpan.LUMIGO_MESSAGE_ID_KEY,
                                UUID.randomUUID()
                                        .toString()
                                        .substring(0, 10)
                                        .getBytes(StandardCharsets.UTF_8));
            } catch (Throwable e) {
                Logger.error(e);
            }
        }

        @AllArgsConstructor
        public static class KafkaProducerCallback<K, V> implements Callback {
            private final Callback callback;
            private final Serializer<K> keySerializer;
            private final Serializer<V> valueSerializer;
            private final ProducerMetadata producerMetadata;
            private final ProducerRecord<K, V> record;
            private final long startTime;

            @Override
            public void onCompletion(RecordMetadata recordMetadata, Exception exception) {
                try {
                    if (callback != null) {
                        callback.onCompletion(recordMetadata, exception);
                    }
                    Logger.info("Handling kafka request {}", record.hashCode());
                    spansContainer.addKafkaProduceSpan(
                            startTime,
                            keySerializer,
                            valueSerializer,
                            producerMetadata,
                            record,
                            recordMetadata,
                            exception);
                } catch (Throwable error) {
                    Logger.error(error, "Failed to add kafka span");
                }
            }
        }
    }
}
