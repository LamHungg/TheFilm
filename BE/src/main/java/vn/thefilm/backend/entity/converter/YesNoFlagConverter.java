package vn.thefilm.backend.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import vn.thefilm.backend.enums.YesNoFlag;

@Converter(autoApply = true)
public class YesNoFlagConverter implements AttributeConverter<YesNoFlag, String> {

    @Override
    public String convertToDatabaseColumn(YesNoFlag attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public YesNoFlag convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        try {
            return YesNoFlag.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Giá trị cờ Y/N không hợp lệ: " + value, exception);
        }
    }
}