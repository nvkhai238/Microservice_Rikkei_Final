package com.ecommerce.common.response;

import com.ecommerce.common.exception.AppException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** FR-ADM-004, NFR-PERF-008: không cho phép truy vấn không giới hạn. */
class PageRequestsTest {

    @Test
    void defaultsWhenMissing() {
        var p = PageRequests.of(null, null, Sort.unsorted());
        assertThat(p.getPageNumber()).isZero();
        assertThat(p.getPageSize()).isEqualTo(PageRequests.DEFAULT_SIZE);
    }

    @Test
    void rejectsHugePageSize() {
        assertThatThrownBy(() -> PageRequests.of(0, 10_000, Sort.unsorted())).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> PageRequests.of(-1, 10, Sort.unsorted())).isInstanceOf(AppException.class);
    }
}
