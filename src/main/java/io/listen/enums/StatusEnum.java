package io.listen.enums;

public enum StatusEnum {

    NORMAL(1),
    DISABLED(0);

    private final Integer value;

    StatusEnum(Integer value) {
        this.value = value;
    }

    public Integer value() {
        return value;
    }

    // 根据value获取枚举
    public static StatusEnum fromValue(Integer value) {
        for (StatusEnum status : StatusEnum.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid status value: " + value);
    }
}
