package com.ecommerce.common.audit;

import com.ecommerce.common.response.PageResponse;
import com.ecommerce.common.security.CurrentUser;
import com.ecommerce.common.web.ClientIp;
import com.ecommerce.common.web.CorrelationId;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;

/**
 * Ghi audit trong CÙNG transaction với thay đổi (MANDATORY): thay đổi rollback thì audit cũng rollback,
 * không có chuyện "audit nói đã đổi giá nhưng giá chưa đổi".
 */
@Service
public class AuditService {

    private final AuditLogRepository repository;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    public AuditService(AuditLogRepository repository, JsonMapper jsonMapper, Clock clock) {
        this.repository = repository;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String action, String targetType, Object targetId, Object before, Object after) {
        record(CurrentUser.actorOrSystem(), action, targetType, targetId, before, after, "SUCCESS");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String actorId, String action, String targetType, Object targetId, Object before,
                       Object after, String result) {
        repository.save(new AuditLog(actorId, action, targetType,
                targetId == null ? null : String.valueOf(targetId),
                toJson(before), toJson(after), result, CorrelationId.current(), ClientIp.current(), clock.instant()));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditEntry> search(String targetType, String targetId, String actorId, Pageable pageable) {
        return PageResponse.of(repository.search(targetType, targetId, actorId, pageable), AuditEntry::from);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof String s ? s : jsonMapper.writeValueAsString(value);
    }

    public record AuditEntry(String id, String actorId, String action, String targetType, String targetId,
                             String before, String after, String result, String correlationId, String clientIp,
                             Instant createdAt) {
        static AuditEntry from(AuditLog a) {
            return new AuditEntry(a.getId().toString(), a.getActorId(), a.getAction(), a.getTargetType(),
                    a.getTargetId(), a.getBeforeData(), a.getAfterData(), a.getResult(), a.getCorrelationId(),
                    a.getClientIp(), a.getCreatedAt());
        }
    }
}
