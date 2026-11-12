package com.ecommerce.order.inventory;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** BR-005: một reservation chỉ được commit HOẶC release đúng một lần. */
class ReservationTest {

    private final Instant now = Instant.now();

    private Reservation active() {
        return new Reservation(UUID.randomUUID(), UUID.randomUUID(), "MAIN", 1, now.plusSeconds(900), now);
    }

    @Test
    void activeCanBeCommittedOnce() {
        Reservation r = active();
        r.moveTo(ReservationStatus.COMMITTED, now);
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.COMMITTED);
        assertThatThrownBy(() -> r.moveTo(ReservationStatus.COMMITTED, now)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> r.moveTo(ReservationStatus.RELEASED, now)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void releasedCannotBeCommitted() {
        Reservation r = active();
        r.moveTo(ReservationStatus.RELEASED, now);
        assertThatThrownBy(() -> r.moveTo(ReservationStatus.COMMITTED, now)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void committedCanOnlyBeRestocked() {
        Reservation r = active();
        r.moveTo(ReservationStatus.COMMITTED, now);
        r.moveTo(ReservationStatus.RESTOCKED, now);
        assertThatThrownBy(() -> r.moveTo(ReservationStatus.RESTOCKED, now)).isInstanceOf(IllegalStateException.class);
    }
}
