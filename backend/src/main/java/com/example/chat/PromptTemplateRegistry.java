package com.example.chat;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class PromptTemplateRegistry {

    private final Map<PromptTemplateId, Definition> definitions;

    public PromptTemplateRegistry(ResourceLoader resourceLoader) {
        this.definitions = Map.of(
                PromptTemplateId.CONCISE,
                new Definition(resourceLoader.getResource("classpath:prompts/concise.st"), Set.of("language")),
                PromptTemplateId.BOOK_EXTRACTOR,
                new Definition(resourceLoader.getResource("classpath:prompts/book-extractor.st"),
                        Set.of("language", "domain")));
    }

    public Definition resolve(String templateId, Map<String, String> variables) {
        PromptTemplateId id = PromptTemplateId.from(templateId);
        Definition definition = definitions.get(id);
        Map<String, String> safeVariables = variables == null ? new java.util.HashMap<>() : new java.util.HashMap<>(variables);
        if (!definition.allowedVariables().containsAll(safeVariables.keySet())) {
            Set<String> unknown = new java.util.HashSet<>(safeVariables.keySet());
            unknown.removeAll(definition.allowedVariables());
            throw new IllegalArgumentException("Unsupported template variables for " + templateId + ": " + unknown);
        }
        for (String variable : definition.allowedVariables()) {
            String value = safeVariables.get(variable);
            if (value == null || value.isBlank()) {
                value = switch (variable) {
                    case "language" -> "same language as user";
                    case "domain" -> "book";
                    default -> null;
                };
                safeVariables.put(variable, value);
            }
            boolean validValue = value != null;
            if (validValue) {
                validValue = switch (variable) {
                    case "language" -> value.matches("[\\p{L}][\\p{L}\\p{N} -]{0,39}");
                    case "domain" -> value.matches("[\\p{L}\\p{N}][\\p{L}\\p{N} _-]{0,59}");
                    default -> value.length() <= 120;
                };
            }
            if (!validValue) {
                throw new IllegalArgumentException("Template variable '" + variable + "' contains an invalid value");
            }
        }
        return new Definition(definition.resource(), definition.allowedVariables(), Map.copyOf(safeVariables));
    }

    public record Definition(Resource resource, Set<String> allowedVariables, Map<String, String> variables) {
        public Definition(Resource resource, Set<String> allowedVariables) {
            this(resource, allowedVariables, Map.of());
        }
    }
}
