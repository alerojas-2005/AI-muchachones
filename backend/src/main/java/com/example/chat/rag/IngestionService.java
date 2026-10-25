package com.example.chat.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pipeline de ingesta (ETL) del RAG:
 *   Extract   -> TikaDocumentReader lee el archivo y extrae el texto.
 *   Transform -> TokenTextSplitter lo divide en fragmentos (chunks).
 *   Load      -> VectorStore calcula el embedding de cada fragmento y lo guarda en PGVector.
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final VectorStore vectorStore;

    // Fragmentos de ~500 tokens: 4 fragmentos recuperados caben holgados en num-ctx=4096.
    private final TokenTextSplitter splitter = TokenTextSplitter.builder()
            .withChunkSize(500)
            .withMinChunkSizeChars(200)
            .build();

    public IngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public int ingest(Resource resource, String fileName) {
        // 1. Extract: Tika detecta el tipo de archivo y extrae el texto plano.
        List<Document> documents = new TikaDocumentReader(resource).read();

        // 2. Transform: dividir en fragmentos.
        List<Document> chunks = splitter.apply(documents);

        // 3. Metadatos: guardamos de que archivo viene cada fragmento.
        List<Document> enriched = chunks.stream()
                .map(chunk -> {
                    Map<String, Object> metadata = new HashMap<>(chunk.getMetadata());
                    metadata.put("fuente", fileName);
                    return new Document(chunk.getText(), metadata);
                })
                .toList();

        // 4. Load: el VectorStore llama al EmbeddingModel y guarda texto + vector + metadatos.
        vectorStore.add(enriched);

        log.info("Documento '{}' indexado: {} fragmentos", fileName, enriched.size());
        return enriched.size();
    }
}