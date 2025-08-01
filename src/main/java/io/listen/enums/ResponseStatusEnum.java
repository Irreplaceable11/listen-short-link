package io.listen.enums;

public enum ResponseStatusEnum {

    OK(0,"successful"),

    ERROR(1, "error");

    private final int code;

    private final String message;


    ResponseStatusEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}
