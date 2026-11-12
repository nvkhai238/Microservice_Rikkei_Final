package com.ecommerce.payment.reconciliation;

import com.ecommerce.common.exception.AppException;
import com.ecommerce.payment.config.PaymentProperties;
import com.ecommerce.payment.payment.Payment;
import com.ecommerce.payment.payment.PaymentRepository;
import com.ecommerce.payment.payment.PaymentStatus;
import com.ecommerce.payment.provider.PaymentProvider;
import com.ecommerce.payment.provider.PaymentProviders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Đối soát định kỳ (FR-PAY-018): so từng payment nội bộ với dữ liệu phía provider, ghi báo cáo sai lệch.
 * Ví dụ sai lệch: provider báo đã thu tiền nhưng mình chưa nhận webhook (webhook bị mất) => cần xử lý tay.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final PaymentRepository payments;
    private final PaymentProviders providers;
    private final JdbcTemplate jdbc;
    private final PaymentProperties props;
    private final Clock clock;

    public record RunView(UUID id, Instant startedAt, Instant finishedAt, int checked, int mismatches,
                          String triggeredBy) {
    }

    public record Item(UUID paymentId, String orderCode, String internalStatus, String providerStatus,
                       BigDecimal internalCaptured, BigDecimal providerCaptured, BigDecimal internalRefunded,
                       BigDecimal providerRefunded, String issue) {
    }

    public record RunDetail(RunView run, List<Item> items) {
    }

    @Scheduled(cron = "${app.payment.reconciliation-cron}")
    public void scheduled() {
        run("SYSTEM");
    }

    @Transactional
    public RunView run(String triggeredBy) {
        UUID runId = UUID.randomUUID();
        Instant started = clock.instant();
        jdbc.update("INSERT INTO reconciliation_runs (id, started_at, triggered_by) VALUES (?, ?, ?)",
                runId, java.sql.Timestamp.from(started), triggeredBy);
        List<Payment> candidates = payments.findByUpdatedAtAfterAndProviderRefIsNotNull(
                started.minus(props.reconciliationLookback()));
        List<Item> mismatches = new ArrayList<>();
        for (Payment p : candidates) {
            PaymentProvider.ProviderStatus ps = providers.get(p.getProvider()).fetchStatus(p.getProviderRef());
            String issue = compare(p, ps);
            if (issue != null) {
                Item item = new Item(p.getId(), p.getOrderCode(), p.getStatus().name(), ps.status(),
                        p.getCapturedAmount(), ps.capturedAmount(), p.getRefundedAmount(), ps.refundedAmount(), issue);
                mismatches.add(item);
                jdbc.update("""
                        INSERT INTO reconciliation_items (id, run_id, payment_id, order_code, internal_status, provider_status,
                            internal_captured, provider_captured, internal_refunded, provider_refunded, issue)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, UUID.randomUUID(), runId, p.getId(), p.getOrderCode(), item.internalStatus(),
                        item.providerStatus(), item.internalCaptured(), item.providerCaptured(),
                        item.internalRefunded(), item.providerRefunded(), issue);
            }
        }
        Instant finished = clock.instant();
        jdbc.update("UPDATE reconciliation_runs SET finished_at = ?, checked = ?, mismatches = ? WHERE id = ?",
                java.sql.Timestamp.from(finished), candidates.size(), mismatches.size(), runId);
        if (!mismatches.isEmpty()) {
            log.error("[ALERT] Reconciliation {} found {} mismatch(es) out of {} payment(s)", runId,
                    mismatches.size(), candidates.size());
        } else {
            log.info("Reconciliation {} checked {} payment(s), no mismatch", runId, candidates.size());
        }
        return new RunView(runId, started, finished, candidates.size(), mismatches.size(), triggeredBy);
    }

    /** null = khớp */
    static String compare(Payment p, PaymentProvider.ProviderStatus ps) {
        boolean providerCaptured = ps.capturedAmount().signum() > 0;
        boolean internalCaptured = p.getStatus().isCaptured();
        if (providerCaptured && !internalCaptured) {
            return "PROVIDER_CAPTURED_BUT_INTERNAL_NOT";   // có thể mất webhook
        }
        if (!providerCaptured && internalCaptured) {
            return "INTERNAL_CAPTURED_BUT_PROVIDER_NOT";
        }
        if (providerCaptured && ps.capturedAmount().compareTo(p.getCapturedAmount()) != 0) {
            return "CAPTURED_AMOUNT_DIFFERS";
        }
        if (ps.refundedAmount().compareTo(p.getRefundedAmount()) != 0 && p.getStatus() != PaymentStatus.REFUND_PENDING) {
            return "REFUNDED_AMOUNT_DIFFERS";
        }
        return null;
    }

    @Transactional(readOnly = true)
    public List<RunView> recentRuns() {
        return jdbc.query("SELECT * FROM reconciliation_runs ORDER BY started_at DESC LIMIT 20", (rs, i) -> new RunView(
                rs.getObject("id", UUID.class), rs.getTimestamp("started_at").toInstant(),
                rs.getTimestamp("finished_at") == null ? null : rs.getTimestamp("finished_at").toInstant(),
                rs.getInt("checked"), rs.getInt("mismatches"), rs.getString("triggered_by")));
    }

    @Transactional(readOnly = true)
    public RunDetail detail(UUID runId) {
        RunView run = recentRuns().stream().filter(r -> r.id().equals(runId)).findFirst()
                .orElseGet(() -> jdbc.query("SELECT * FROM reconciliation_runs WHERE id = ?", (rs, i) -> new RunView(
                        rs.getObject("id", UUID.class), rs.getTimestamp("started_at").toInstant(),
                        rs.getTimestamp("finished_at") == null ? null : rs.getTimestamp("finished_at").toInstant(),
                        rs.getInt("checked"), rs.getInt("mismatches"), rs.getString("triggered_by")), runId)
                        .stream().findFirst().orElseThrow(() -> AppException.notFound("Reconciliation run")));
        List<Item> items = jdbc.query("SELECT * FROM reconciliation_items WHERE run_id = ?", (rs, i) -> new Item(
                rs.getObject("payment_id", UUID.class), rs.getString("order_code"), rs.getString("internal_status"),
                rs.getString("provider_status"), rs.getBigDecimal("internal_captured"),
                rs.getBigDecimal("provider_captured"), rs.getBigDecimal("internal_refunded"),
                rs.getBigDecimal("provider_refunded"), rs.getString("issue")), runId);
        return new RunDetail(run, items);
    }
}
