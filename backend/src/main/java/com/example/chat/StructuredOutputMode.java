package com.example.chat;

public enum StructuredOutputMode {
    FRAMEWORK_VALIDATION,
    PROVIDER_SCHEMA,
    BOTH;

    public static StructuredOutputMode from(String value) {
        try {
            return value == null ? null : valueOf(value);
        }
        catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown structuredOutputMode: " + value);
        }
    }
}
