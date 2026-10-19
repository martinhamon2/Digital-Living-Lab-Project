package io.github.martinhamon2.steamdeck_app.api;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({NoSuchElementException.class})
    ResponseEntity<Map<String, String>> notFound(Exception e) { return error(HttpStatus.NOT_FOUND, e); }
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    ResponseEntity<Map<String, String>> badRequest(Exception e) { return error(HttpStatus.BAD_REQUEST, e); }
    private ResponseEntity<Map<String, String>> error(HttpStatus status, Exception e) {
        return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
    }
}
