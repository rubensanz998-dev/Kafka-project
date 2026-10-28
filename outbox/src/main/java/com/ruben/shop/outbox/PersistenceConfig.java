package com.ruben.shop.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Las entidades y repositorios están en dos paquetes (el del servicio y este), así que se escanea la raíz. */
@Configuration
@EntityScan("com.ruben.shop")
@EnableJpaRepositories("com.ruben.shop")
public class PersistenceConfig {

    /** Los tests la desactivan con outbox.scheduling.enabled=false y llaman a publishPending() a mano. */
    @Configuration
    @EnableScheduling
    @ConditionalOnProperty(name = "outbox.scheduling.enabled", matchIfMissing = true)
    static class SchedulingConfig {
    }
}
