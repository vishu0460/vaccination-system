package com.vaccine.dto;

import java.util.Set;

public class AuthResponse {
    
    private String token;
    private String refreshToken;
    private Long expiresIn;
    private UserDTO user;
    
    public AuthResponse() {}
    
    public AuthResponse(String token, String refreshToken, Long expiresIn, UserDTO user) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.user = user;
    }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public Long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(Long expiresIn) { this.expiresIn = expiresIn; }
    public UserDTO getUser() { return user; }
    public void setUser(UserDTO user) { this.user = user; }
}
