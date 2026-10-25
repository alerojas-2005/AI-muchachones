package com.example.chat.dto;

/** Respuesta del endpoint de carga: cuantos fragmentos quedaron indexados. */
public record UploadResponse(String fileName, int chunks, long elapsedMs, String status) {}