package com.dockerforjavadevelopers.hello.controller;

import com.example.javaapp1.model.Message;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

@RestController
@RequestMapping("/api")
@Tag(name = "Hello", description = "Simple greeting and health endpoints")
public class HelloController {

    private static final Logger log = LoggerFactory.getLogger(HelloController.class);

    @GetMapping("/hello")
    @Operation(summary = "Say hello", description = "Returns a greeting message with optional name parameter")
    public ResponseEntity<Message> hello(
            @RequestParam(required = false, defaultValue = "World") String name) {
        log.info("GET /api/hello name={}", name);
        Message message = new Message(
                "Hello, " + name + "!",
                Instant.now().toString()
        );
        return ResponseEntity.ok(message);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Simple health indicator for the application")
    public ResponseEntity<Message> health() {
        log.info("GET /api/health");
        return ResponseEntity.ok(new Message("UP", Instant.now().toString()));
    }
}
