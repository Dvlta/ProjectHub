package com.leorsun.projecthub.responses;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {
    private String token;
    private long expiresIn;
    private String refreshToken;
    private long refreshExpiresIn;

    public LoginResponse(String token, long expiresIn, String refreshToken, long refreshExpiresIn) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.refreshToken = refreshToken;
        this.refreshExpiresIn = refreshExpiresIn;
    }
}
