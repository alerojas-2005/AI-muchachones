package com.example.chat;

public enum PromptTemplateId {
    CONCISE("concise"),
    BOOK_EXTRACTOR("book-extractor");

    private final String value;

    PromptTemplateId(String value) { this.value = value; }

    public String value() { return value; }

    public static PromptTemplateId from(String value) {
        for (PromptTemplateId id : values()) {
            if (id.value.equals(value)) return id;
        }
        throw new IllegalArgumentException("Unknown promptTemplateId: " + value);
    }
}
