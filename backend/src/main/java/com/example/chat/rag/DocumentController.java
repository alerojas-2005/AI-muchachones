package com.example.chat.rag;

import com.example.chat.dto.UploadResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final IngestionService ingestionService;

    public DocumentController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new UploadResponse(file.getOriginalFilename(), 0, 0, "error"));
        }

        long start = System.currentTimeMillis();
        int chunks = ingestionService.ingest(file.getResource(), file.getOriginalFilename());
        long elapsed = System.currentTimeMillis() - start;

        return ResponseEntity.ok(new UploadResponse(file.getOriginalFilename(), chunks, elapsed, "ok"));
    }
}