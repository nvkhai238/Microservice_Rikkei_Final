package com.ecommerce.common.response;

import com.ecommerce.common.exception.AppException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Tạo PageRequest an toàn: page >= 0, size trong [1, MAX_SIZE] (FR-ADM-004, NFR-PERF-008).
 * Client gửi size=1000000 cũng chỉ nhận tối đa MAX_SIZE bản ghi.
 */
public final class PageRequests {
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private PageRequests() {
    }

    public static PageRequest of(Integer page, Integer size, Sort sort) {
        int p = page == null ? 0 : page;
        int s = size == null ? DEFAULT_SIZE : size;
        if (p < 0) {
            throw AppException.validation("page", "MIN_0");
        }
        if (s < 1 || s > MAX_SIZE) {
            throw AppException.validation("size", "RANGE_1_" + MAX_SIZE);
        }
        return PageRequest.of(p, s, sort);
    }
}
