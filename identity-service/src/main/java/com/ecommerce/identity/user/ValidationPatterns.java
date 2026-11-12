package com.ecommerce.identity.user;

/** Regex dùng chung cho validate DTO. */
public final class ValidationPatterns {
    /** 0xxxxxxxxx, +84xxxxxxxxx, 84xxxxxxxxx (9-10 số sau đầu số), cho phép dấu cách/chấm/gạch */
    public static final String VN_PHONE = "^(\\+?84|0)[\\s.-]?\\d([\\s.-]?\\d){8,9}$";

    private ValidationPatterns() {
    }
}
