package com.riskmanager.auth;

import com.riskmanager.user.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    @Builder.Default
    private String tokenType = "Bearer";
    private String id;
    private String name;
    private String email;
    private Role role;

    public AuthResponse(String token, String id, String name, String email, Role role) {
        this.token = token;
        this.tokenType = "Bearer";
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
    }
}
