package vn.thefilm.backend.service;

public interface EmailVerificationService {
    long CODE_TTL_SECONDS = 600;

    void issueCode(long appUserId, String email, String displayName);

    boolean verifyCode(long appUserId, String code);
}
