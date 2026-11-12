package com.ecommerce.common.security;

/**
 * Vai trò tối thiểu. Trong JWT claim "roles" chứa tên KHÔNG có tiền tố,
 * Spring Security đổi thành authority "ROLE_xxx" để dùng với hasRole('xxx').
 */
public final class Roles {
    public static final String CUSTOMER = "CUSTOMER";
    public static final String CATALOG_STAFF = "CATALOG_STAFF";
    public static final String INVENTORY_STAFF = "INVENTORY_STAFF";
    public static final String ORDER_STAFF = "ORDER_STAFF";
    public static final String FINANCE_STAFF = "FINANCE_STAFF";
    public static final String ADMIN = "ADMIN";

    private Roles() {
    }
}
