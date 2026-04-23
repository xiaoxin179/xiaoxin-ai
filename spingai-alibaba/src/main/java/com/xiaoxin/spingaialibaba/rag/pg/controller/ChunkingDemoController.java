package com.xiaoxin.spingaialibaba.rag.pg.controller;

import com.xiaoxin.spingaialibaba.rag.pg.service.ChunkingDemoService;
import com.xiaoxin.spingaialibaba.rag.pg.service.DocumentLoaderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
//文档切分
@Slf4j
@RestController
@RequestMapping("/api/chunk")
public class ChunkingDemoController {

    private final DocumentLoaderService loaderService;
    private final ChunkingDemoService chunkingService;

    public ChunkingDemoController(DocumentLoaderService loaderService,
                                  ChunkingDemoService chunkingService) {
        this.loaderService = loaderService;
        this.chunkingService = chunkingService;
    }

    // 加载 PDF 并按 Token 切片，返回切片数量和第一段预览
    @GetMapping("/pdf")
    public Map<String, Object> chunkPdf(@RequestParam String filename) {
        List<Document> docs = loaderService.loadPdfByPage(filename);
        List<Document> chunks = chunkingService.splitByToken(docs);
        return Map.of(
                "rawCount", docs.size(),
                "chunkCount", chunks.size(),
                "firstChunk", chunks.isEmpty() ? "" : chunks.get(0).getText().substring(0, Math.min(200, chunks.get(0).getText().length()))
        );
    }

    // 加载文本并按段落切片
    @GetMapping("/text")
    public Map<String, Object> chunkText(@RequestParam String filename) {
        List<Document> docs = loaderService.loadText(filename);
        List<Document> chunks = chunkingService.splitByParagraph(docs);
        return Map.of(
                "chunkCount", chunks.size(),
                "firstChunk", chunks.isEmpty() ? "" : chunks.get(0).getText()
        );
    }

    // 使用 Tika 加载任意格式文件（支持 Word/Excel/HTML/MD 等）
    @GetMapping("/tika")
    public Map<String, Object> chunkWithTika(@RequestParam String path) {
        List<Document> docs = loaderService.loadWithTika(path);
        List<Document> chunks = chunkingService.splitByParagraph(docs);
        log.info("=== Tika 切片结果 (共 {} 段) ===", chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            String text = chunks.get(i).getText();
            log.info("--- Chunk {} (长度: {} 字符) ---", i + 1, text.length());
            log.info(text);
            log.info("--- End Chunk {} ---", i + 1);
        }
        return Map.of(
                "rawCount", docs.size(),
                "chunkCount", chunks.size(),
                "firstChunk", chunks.isEmpty() ? "" : chunks.get(0).getText()
        );
    }
}