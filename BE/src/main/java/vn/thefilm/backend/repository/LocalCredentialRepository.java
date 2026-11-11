package vn.thefilm.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Lưu mật khẩu đăng nhập thử nghiệm, chỉ tồn tại ở profile local (H2).
 * Ở profile oracle, danh tính và mật khẩu do Keycloak quản lý nên bean này không được tạo.
 */
@Repository
@Profile("local")
public class LocalCredentialRepository {

    private final JdbcTemplate jdbcTemplate;

    public LocalCredentialRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(Long appUserId, String passwordHash) {
        jdbcTemplate.update(
                "INSERT INTO local_credential (app_user_id, password_hash) VALUES (?, ?)",
                appUserId, passwordHash);
    }

    public Optional<String> findPasswordHash(Long appUserId) {
        List<String> hashes = jdbcTemplate.queryForList(
                "SELECT password_hash FROM local_credential WHERE app_user_id = ?",
                String.class, appUserId);
        return hashes.stream().findFirst();
    }
}
