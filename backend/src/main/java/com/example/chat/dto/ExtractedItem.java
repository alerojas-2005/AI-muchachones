package com.example.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record ExtractedItem(
        @JsonProperty(required = true)
        @JsonPropertyDescription("The item title")
        String title,
        @JsonProperty(required = true)
        @JsonPropertyDescription("A concise summary of the item")
        String summary) {
}
