package vn.thefilm.backend.service.impl;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.thefilm.backend.service.AuditLogService;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final Pattern CREDENTIAL_ASSIGNMENT = Pattern.compile(
            "(?i)([\\\"']?(?:password|passwd|pwd|passphrase|secret|client[_-]?secret|access[_-]?token|refresh[_-]?token|id[_-]?token|"
                    + "authorization|credential|api[_-]?key|private[_-]?key|signing[_-]?key|cookie)"
                    + "[\\\"']?\\s*[:=]\\s*[\\\"']?)([^\\s,;\\\"'}]+)");
    private static final Pattern BEARER_TOKEN = Pattern.compile("(?i)\\bBearer\\s+[^\\s,;]+");
    private static final Pattern JWT = Pattern.compile("\\b[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\b");

    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    public AuditLogServiceImpl(EntityManager entityManager, ObjectMapper objectMapper) {
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void recordSecurityEvent(
            Long actorUserId,
            String actionCode,
            String objectType,
            String objectId,
            String traceId,
            Map<String, ?> beforeData,
            Map<String, ?> afterData,
            String sourceIp) {
        Query query = entityManager.createNativeQuery("""
                INSERT INTO audit_event (
                    actor_user_id, action_code, object_type, object_id,
                    trace_id, before_data, after_data, source_ip, occurred_at
                ) VALUES (
                    :actorUserId, :actionCode, :objectType, :objectId,
                    :traceId, :beforeData, :afterData, :sourceIp, :occurredAt
                )
                """);
        query.setParameter("actorUserId", actorUserId);
        query.setParameter("actionCode", sanitizeText(actionCode));
        query.setParameter("objectType", sanitizeText(objectType));
        query.setParameter("objectId", sanitizeText(objectId));
        query.setParameter("traceId", sanitizeText(traceId));
        query.setParameter("beforeData", toSafeJson(beforeData));
        query.setParameter("afterData", toSafeJson(afterData));
        query.setParameter("sourceIp", sanitizeText(sourceIp));
        query.setParameter("occurredAt", OffsetDateTime.now());
        query.executeUpdate();
    }

    private String toSafeJson(Map<String, ?> data) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(sanitizeMap(data));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Audit event data could not be serialized safely.", exception);
        }
    }

    private Map<String, Object> sanitizeMap(Map<?, ?> source) {
        Map<String, Object> safe = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            String safeKey = sanitizeText(String.valueOf(key));
            if (isSensitiveKey(safeKey)) {
                safe.put(safeKey, "[REDACTED]");
            } else {
                safe.put(safeKey, sanitizeValue(value));
            }
        });
        return safe;
    }

    private Object sanitizeValue(Object value) {
        if (value instanceof Map<?, ?> nestedMap) {
            return sanitizeMap(nestedMap);
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> safe = new ArrayList<>();
            iterable.forEach(item -> safe.add(sanitizeValue(item)));
            return safe;
        }
        if (value instanceof String text) {
            return sanitizeText(text);
        }
        if (value instanceof Number || value instanceof Boolean || value == null) {
            return value;
        }
        if (value instanceof Enum<?> enumValue) {
            return sanitizeText(enumValue.name());
        }
        return "[REDACTED_UNSUPPORTED_VALUE]";
    }

    private String sanitizeText(String value) {
        if (value == null) {
            return null;
        }
        String withoutBearerOrJwt = JWT.matcher(BEARER_TOKEN.matcher(value).replaceAll("[REDACTED]"))
                .replaceAll("[REDACTED]");
        return CREDENTIAL_ASSIGNMENT.matcher(withoutBearerOrJwt).replaceAll("$1[REDACTED]");
    }

    private boolean isSensitiveKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        return normalized.contains("pass") || normalized.equals("pwd")
                || normalized.contains("secret") || normalized.contains("token")
                || normalized.contains("authorization") || normalized.contains("credential")
                || normalized.equals("cookie") || normalized.contains("apikey")
                || normalized.contains("privatekey") || normalized.contains("signingkey")
                || normalized.contains("passphrase");
    }
}
