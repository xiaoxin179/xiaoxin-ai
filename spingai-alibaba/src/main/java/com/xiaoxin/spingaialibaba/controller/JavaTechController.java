package com.xiaoxin.spingaialibaba.controller;

import com.xiaoxin.spingaialibaba.service.JavaTechService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/java-tech")
public class JavaTechController {

    private final JavaTechService javaTechService;

    public JavaTechController(JavaTechService jt) {
        this.javaTechService = jt;
    }

    @GetMapping("/ask")
    public String ask(@RequestParam String question) {
        return javaTechService.ask(question);
    }
}