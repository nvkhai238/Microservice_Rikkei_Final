package com.ecommerce.common.messaging;

import org.springframework.amqp.core.Declarables;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Mọi service đều khai báo toàn bộ topology (lý do: xem javadoc của {@link EventTopology}). */
@Configuration(proxyBeanMethods = false)
public class MessagingTopologyConfig {

    @Bean
    public Declarables eventTopology() {
        return EventTopology.all();
    }
}
