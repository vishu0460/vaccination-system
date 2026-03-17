package com.vaccine.common.dto;

public record ApiMessage(
    String message,
    String type
) {
    public ApiMessage(String message) {
        this(message, "SUCCESS");
    }
}
