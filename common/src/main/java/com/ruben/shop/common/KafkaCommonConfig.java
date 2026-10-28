package com.ruben.shop.common;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

/** Configuración compartida por todos los servicios: topics y manejo de errores con DLT. */
@Configuration
public class KafkaCommonConfig {

    /** 3 particiones: permite escalar cada consumer group hasta 3 instancias. */
    public static final int PARTITIONS = 3;

    @Bean
    public KafkaAdmin.NewTopics sagaTopics() {
        // Los DLT se crean con las mismas particiones: el recoverer publica en la misma partición que el original.
        return new KafkaAdmin.NewTopics(Topics.ALL.stream()
                .flatMap(t -> Stream.of(t, t + Topics.DLT_SUFFIX))
                .map(t -> TopicBuilder.name(t).partitions(PARTITIONS).replicas(1).build())
                .toArray(NewTopic[]::new));
    }

    /**
     * 2 reintentos con 1s de espera y después el mensaje va a "topic-dlt".
     * Los mensajes que no se pueden deserializar (poison pills) van directos al DLT, sin reintentos.
     * Boot registra este bean en la listener container factory por defecto.
     */
    @Bean
    public CommonErrorHandler kafkaErrorHandler(ProducerFactory<?, ?> producerFactory) {
        // Un poison pill llega como byte[] y debe ir al DLT tal cual, no re-serializado como JSON.
        Map<Class<?>, Serializer<?>> serializers = new LinkedHashMap<>();
        serializers.put(byte[].class, new ByteArraySerializer());
        serializers.put(Object.class, new JsonSerializer<>());
        var dltTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                producerFactory.getConfigurationProperties(),
                new StringSerializer(),
                new DelegatingByTypeSerializer(serializers, true)));
        return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(dltTemplate), new FixedBackOff(1000L, 2));
    }
}
