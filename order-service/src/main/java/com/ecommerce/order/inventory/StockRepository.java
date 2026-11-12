package com.ecommerce.order.inventory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Bảng stocks chỉ được ghi qua các câu UPDATE CÓ ĐIỀU KIỆN dưới đây (atomic conditional update - FR-INV-009).
 * <p>Vì sao KHÔNG làm kiểu "đọc available, if (available >= q) thì save()"?
 * Hai request đọc cùng lúc đều thấy available = 1, cùng save => bán 2 cái khi chỉ có 1 (oversell).
 * <p>Ở đây điều kiện và việc ghi nằm trong MỘT câu SQL: Postgres khóa dòng khi UPDATE, request thứ hai phải chờ
 * request thứ nhất commit rồi mới đánh giá lại điều kiện với dữ liệu MỚI => chỉ một request thắng.
 * Không cần SELECT FOR UPDATE, không cần distributed lock.
 * <p>RETURNING trả về số mới ngay trong cùng câu lệnh để ghi sổ cái (ledger).
 */
@Repository
public class StockRepository {

    private static final RowMapper<StockLevel> MAPPER = (rs, i) -> new StockLevel(
            rs.getObject("sku_id", UUID.class), rs.getString("warehouse_code"), rs.getLong("on_hand"),
            rs.getLong("reserved"), rs.getLong("version"));
    private static final String RETURNING = " RETURNING sku_id, warehouse_code, on_hand, reserved, version";

    private final JdbcTemplate jdbc;

    public StockRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void ensureExists(UUID skuId, String warehouse) {
        jdbc.update("""
                INSERT INTO stocks (sku_id, warehouse_code, on_hand, reserved, version, updated_at)
                VALUES (?, ?, 0, 0, 0, now()) ON CONFLICT DO NOTHING
                """, skuId, warehouse);
    }

    public Optional<StockLevel> find(UUID skuId, String warehouse) {
        return jdbc.query("SELECT * FROM stocks WHERE sku_id = ? AND warehouse_code = ?", MAPPER, skuId, warehouse)
                .stream().findFirst();
    }

    public List<StockLevel> findAll(List<UUID> skuIds, String warehouse) {
        if (skuIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", skuIds.stream().map(id -> "?").toList());
        Object[] args = new Object[skuIds.size() + 1];
        args[0] = warehouse;
        for (int i = 0; i < skuIds.size(); i++) {
            args[i + 1] = skuIds.get(i);
        }
        return jdbc.query("SELECT * FROM stocks WHERE warehouse_code = ? AND sku_id IN (" + placeholders + ")",
                MAPPER, args);
    }

    public List<StockLevel> page(int limit, long offset) {
        return jdbc.query("SELECT * FROM stocks ORDER BY updated_at DESC, sku_id LIMIT ? OFFSET ?", MAPPER, limit, offset);
    }

    public long count() {
        Long c = jdbc.queryForObject("SELECT count(*) FROM stocks", Long.class);
        return c == null ? 0 : c;
    }

    /** Giữ hàng: chỉ thành công nếu còn đủ available. Empty = không đủ hàng. */
    public Optional<StockLevel> tryReserve(UUID skuId, String warehouse, long qty) {
        return single("""
                UPDATE stocks SET reserved = reserved + ?, version = version + 1, updated_at = now()
                WHERE sku_id = ? AND warehouse_code = ? AND on_hand - reserved >= ?
                """, qty, skuId, warehouse, qty);
    }

    /** Trả hàng đang giữ (đơn hủy / hết hạn). */
    public Optional<StockLevel> release(UUID skuId, String warehouse, long qty) {
        return single("""
                UPDATE stocks SET reserved = reserved - ?, version = version + 1, updated_at = now()
                WHERE sku_id = ? AND warehouse_code = ? AND reserved >= ?
                """, qty, skuId, warehouse, qty);
    }

    /** Thanh toán xong: trừ tồn chính thức = giảm cả on_hand lẫn reserved (FR-INV-012). */
    public Optional<StockLevel> commit(UUID skuId, String warehouse, long qty) {
        return single("""
                UPDATE stocks SET on_hand = on_hand - ?, reserved = reserved - ?, version = version + 1, updated_at = now()
                WHERE sku_id = ? AND warehouse_code = ? AND reserved >= ? AND on_hand >= ?
                """, qty, qty, skuId, warehouse, qty, qty);
    }

    /** Nhập kho / hoàn kho: tăng on_hand. */
    public Optional<StockLevel> addOnHand(UUID skuId, String warehouse, long qty) {
        return single("""
                UPDATE stocks SET on_hand = on_hand + ?, version = version + 1, updated_at = now()
                WHERE sku_id = ? AND warehouse_code = ?
                """, qty, skuId, warehouse);
    }

    /** Điều chỉnh (+/-): không được làm on_hand nhỏ hơn số đang giữ (FR-INV-005). */
    public Optional<StockLevel> adjust(UUID skuId, String warehouse, long delta) {
        return single("""
                UPDATE stocks SET on_hand = on_hand + ?, version = version + 1, updated_at = now()
                WHERE sku_id = ? AND warehouse_code = ? AND on_hand + ? >= reserved
                """, delta, skuId, warehouse, delta);
    }

    private Optional<StockLevel> single(String sql, Object... args) {
        return jdbc.query(sql + RETURNING, MAPPER, args).stream().findFirst();
    }
}
