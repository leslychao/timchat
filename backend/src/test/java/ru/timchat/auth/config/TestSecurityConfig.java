package ru.timchat.auth.config;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@Configuration
@Profile("test")
public class TestSecurityConfig {

  @Bean
  public JwtDecoder jwtDecoder() {
    return token -> Jwt.withTokenValue(token)
        .header("alg", "none")
        .subject("00000000-0000-0000-0000-000000000001")
        .claim("preferred_username", "testuser")
        .claim("realm_access", Map.of("roles", List.of("MEMBER")))
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build();
  }
}
