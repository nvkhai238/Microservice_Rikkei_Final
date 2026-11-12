package com.ecommerce.identity.admin;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Danh mục role cố định (seed trong migration) => đọc bằng JDBC cho gọn, không cần entity. */
@Repository
public class RoleRepository {
    private final JdbcTemplate jdbc;

    public RoleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<AdminDtos.RoleResponse> findAll() {
        return jdbc.query("SELECT code, description FROM roles ORDER BY code",
                (rs, i) -> new AdminDtos.RoleResponse(rs.getString("code"), rs.getString("description")));
    }

    public boolean exists(String code) {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM roles WHERE code = ?", Integer.class, code);
        return count != null && count > 0;
    }
}
