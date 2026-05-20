package com.benchify.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private String token;
    private String refreshToken;
    private Long id;
    private String email;
    private String nombre;
    private String rol;
}
