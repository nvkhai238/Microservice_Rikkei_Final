package com.ecommerce.order.order;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Snapshot địa chỉ + liên hệ (FR-ORD-005): khách sửa sổ địa chỉ sau đó cũng không đổi đơn cũ. */
@Embeddable
public record ShippingAddress(
        @Column(name = "ship_recipient_name", nullable = false) String recipientName,
        @Column(name = "ship_phone", nullable = false) String phone,
        @Column(name = "ship_line1", nullable = false) String line1,
        @Column(name = "ship_ward", nullable = false) String ward,
        @Column(name = "ship_district", nullable = false) String district,
        @Column(name = "ship_province", nullable = false) String province,
        @Column(name = "ship_postal_code") String postalCode) {
}
