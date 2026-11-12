package com.ecommerce.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter chạy ĐẦU TIÊN (trước cả Spring Security) để mọi log, kể cả log lỗi 401, đều có correlationId.
 * <ol>
 *   <li>Đọc header X-Correlation-Id (gateway đã cấp), không có hoặc không hợp lệ thì tạo mới.</li>
 *   <li>Đưa vào MDC + gắn vào response header.</li>
 *   <li>{@code finally} xóa MDC: thread của Tomcat được TÁI SỬ DỤNG cho request sau,
 *       không xóa thì request sau sẽ "mượn" nhầm id của request trước.</li>
 * </ol>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String incoming = request.getHeader(CorrelationId.HEADER);
        String correlationId = CorrelationId.isValid(incoming) ? incoming : CorrelationId.newId();
        CorrelationId.set(correlationId);
        response.setHeader(CorrelationId.HEADER, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            CorrelationId.clear();
        }
    }
}
