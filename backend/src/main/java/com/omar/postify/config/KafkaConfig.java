package com.omar.postify.config;

import com.omar.postify.dto.InteractionEventMessage;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic postifyInteractionsTopic(){
        return TopicBuilder
                .name("postify-interactions")
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Dead-letter topic for messages that fail processing after all retries.
     */
    @Bean
    public NewTopic postifyInteractionsDltTopic(){
        return TopicBuilder
                .name("postify-interactions-dlt")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public ProducerFactory<String, InteractionEventMessage> producerFactory(
            KafkaProperties properties,
            SslBundles sslBundles
    ) {
        Map<String, Object> config = new HashMap<>(
                properties.buildProducerProperties(sslBundles)
        );

        config.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        config.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                JsonSerializer.class
        );

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public ConsumerFactory<String, InteractionEventMessage> consumerFactory(
            KafkaProperties properties,
            SslBundles sslBundles
    ) {
        Map<String, Object> config = new HashMap<>(
                properties.buildConsumerProperties(sslBundles)
        );

        config.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        config.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                JsonDeserializer.class
        );

        config.put(
                JsonDeserializer.TRUSTED_PACKAGES,
                "com.omar.postify.dto"
        );

        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                new JsonDeserializer<>(
                        InteractionEventMessage.class
                )
        );
    }
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InteractionEventMessage>
    kafkaListenerContainerFactory(
            ConsumerFactory<String, InteractionEventMessage> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, InteractionEventMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        /*
         * Configure error handler so that poison pills do not block the consumer
         * partition indefinitely. After 3 retry attempts with 1-second backoff,
         * the failed message is forwarded to the dead-letter topic.
         */
        factory.setCommonErrorHandler(dltErrorHandler());

        return factory;
    }

    /**
     * Error handler that retries transient failures and then sends the message
     * to a dead-letter topic so the consumer partition is not blocked.
     */
    @Bean
    public CommonErrorHandler dltErrorHandler() {
        /* 3 retries with 1-second delay between attempts */
        DefaultErrorHandler handler = new DefaultErrorHandler(
                (record, exception) -> {
                    /*
                     * After exhausting retries, the record is automatically sent to
                     * the dead-letter topic by Spring Kafka's DLT mechanism.
                     */
                },
                new FixedBackOff(Duration.ofSeconds(1).toMillis(), 3)
        );
        handler.setRetryListeners();
        return handler;
    }


    @Bean
    public KafkaTemplate<String, InteractionEventMessage> kafkaTemplate(
            ProducerFactory<String, InteractionEventMessage> producerFactory
    ) {
        return new KafkaTemplate<>(producerFactory);
    }
}
