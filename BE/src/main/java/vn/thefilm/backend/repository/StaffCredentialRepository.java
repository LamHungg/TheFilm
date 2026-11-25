package vn.thefilm.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"local", "oracle"})
public class StaffCredentialRepository {

    private final JdbcTemplate jdbcTemplate;

    public StaffCredentialRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(Long staffAccountId, String passwordHash) {
        jdbcTemplate.update(
                "INSERT INTO staff_credential (staff_account_id, password_hash) VALUES (?, ?)",
                staffAccountId, passwordHash);
    }

    public Optional<String> findPasswordHash(Long staffAccountId) {
        List<String> hashes = jdbcTemplate.queryForList(
                "SELECT password_hash FROM staff_credential WHERE staff_account_id = ?",
                String.class, staffAccountId);
        return hashes.stream().findFirst();
    }
}
