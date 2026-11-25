package vn.thefilm.backend.service.impl;

import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import vn.thefilm.backend.service.EmailVerificationService;

@Service
public class EmailVerificationServiceImpl implements EmailVerificationService {
    private static final int MAX_ATTEMPTS = 5;

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final SecureRandom random = new SecureRandom();
    private final String fromAddress;

    public EmailVerificationServiceImpl(JdbcTemplate jdbc, PasswordEncoder passwordEncoder,
            JavaMailSender mailSender, @Value("${app.mail.from:no-reply@thefilm.vn}") String fromAddress) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    @Transactional
    public void issueCode(long appUserId, String email, String displayName) {
        Integer recentlySent = jdbc.queryForObject("SELECT COUNT(*) FROM email_verification_challenge "
                        + "WHERE app_user_id = ? AND created_at > SYSTIMESTAMP - INTERVAL '60' SECOND",
                Integer.class, appUserId);
        if (recentlySent != null && recentlySent > 0) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Vui lòng đợi 60 giây trước khi yêu cầu mã xác minh mới.");
        }
        String code = "%06d".formatted(random.nextInt(1_000_000));
        jdbc.update("DELETE FROM email_verification_challenge WHERE app_user_id = ?", appUserId);
        jdbc.update("INSERT INTO email_verification_challenge "
                        + "(app_user_id, code_hash, expires_at, attempts, created_at) "
                        + "VALUES (?, ?, SYSTIMESTAMP + INTERVAL '10' MINUTE, 0, SYSTIMESTAMP)",
                appUserId, passwordEncoder.encode(code));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("Xác minh email THEFILM");
        message.setText("Xin chào " + (displayName == null ? "bạn" : displayName) + ",\n\n"
                + "Mã xác minh email THEFILM của bạn là: " + code + "\n"
                + "Mã có hiệu lực trong 10 phút và chỉ dùng được một lần.\n\n"
                + "Nếu bạn không yêu cầu, hãy bỏ qua email này.");
        mailSender.send(message);
    }

    @Override
    @Transactional
    public boolean verifyCode(long appUserId, String code) {
        var rows = jdbc.query("SELECT code_hash, attempts, "
                        + "CASE WHEN expires_at > SYSTIMESTAMP THEN 1 ELSE 0 END AS not_expired "
                        + "FROM email_verification_challenge WHERE app_user_id = ? FOR UPDATE",
                (rs, rowNum) -> new VerificationRow(rs.getString("code_hash"), rs.getInt("attempts"),
                        rs.getInt("not_expired") == 1), appUserId);
        if (rows.isEmpty()) return false;

        VerificationRow challenge = rows.get(0);
        if (!challenge.notExpired() || challenge.attempts() >= MAX_ATTEMPTS) return false;
        if (!passwordEncoder.matches(code, challenge.codeHash())) {
            jdbc.update("UPDATE email_verification_challenge SET attempts = attempts + 1 WHERE app_user_id = ?",
                    appUserId);
            return false;
        }

        jdbc.update("UPDATE app_user SET email_verified = 'Y', updated_at = SYSTIMESTAMP WHERE app_user_id = ?",
                appUserId);
        jdbc.update("DELETE FROM email_verification_challenge WHERE app_user_id = ?", appUserId);
        return true;
    }

    private record VerificationRow(String codeHash, int attempts, boolean notExpired) { }
}
