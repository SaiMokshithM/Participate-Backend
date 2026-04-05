package com.participatehere.controller;

import com.participatehere.dto.request.AiRequest;
import com.participatehere.dto.response.AiResponse;
import com.participatehere.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    /**
     * POST /api/ai/chat
     * Requires: valid JWT (any authenticated student or admin)
     * Body: { "message": "string" }
     * Returns: { "reply": "string" }
     */
    @PostMapping("/chat")
    public ResponseEntity<AiResponse> chat(@Valid @RequestBody AiRequest request) {
        String reply = aiService.chat(request.message());
        return ResponseEntity.ok(new AiResponse(reply));
    }
}
