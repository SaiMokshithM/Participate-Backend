package com.participatehere.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiRequest(
    @NotBlank(message = "Message cannot be blank")
    @Size(max = 500, message = "Message must be 500 characters or less")
    String message
) {}
