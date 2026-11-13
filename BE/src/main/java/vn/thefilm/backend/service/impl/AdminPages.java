package vn.thefilm.backend.service.impl;

import org.springframework.data.domain.*;
import vn.thefilm.backend.dto.PageResponse;

final class AdminPages {
    private AdminPages() { }
    static Pageable request(int page, int size, String sort) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("page >= 0; size từ 1 đến 100.");
        return PageRequest.of(page, size, Sort.by(sort).descending());
    }
    static <T> PageResponse<T> response(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
    static String pattern(String value) {
        return "%" + value.trim().toLowerCase(java.util.Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }
}
