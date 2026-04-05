package com.participatehere.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Identifier is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    private String role;
}
