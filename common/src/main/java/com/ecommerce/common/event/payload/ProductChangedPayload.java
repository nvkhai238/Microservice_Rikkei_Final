package com.ecommerce.common.event.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductChangedPayload(UUID productId, long version, String changeType) {
}
