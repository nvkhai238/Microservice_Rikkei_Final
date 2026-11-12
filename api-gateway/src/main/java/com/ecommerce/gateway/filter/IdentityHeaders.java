package com.ecommerce.gateway.filter;

public final class IdentityHeaders {
    public static final String USER_ID = "X-User-Id";
    public static final String USER_ROLES = "X-User-Roles";
    public static final String USER_ID_ATTR = "gateway.userId";

    private IdentityHeaders() {
    }
}
