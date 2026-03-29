package ru.timchat.auth.config;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void requestWithoutToken_returns401() throws Exception {
    mockMvc.perform(get("/api/auth-test/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code", is("UNAUTHORIZED")))
        .andExpect(jsonPath("$.message", notNullValue()))
        .andExpect(jsonPath("$.traceId", notNullValue()));
  }

  @Test
  void requestWithValidJwt_returns200() throws Exception {
    mockMvc.perform(get("/api/auth-test/me")
            .with(jwt().jwt(builder -> builder
                .subject("00000000-0000-0000-0000-000000000001")
                .claim("preferred_username", "testuser")
                .claim("realm_access",
                    Map.of("roles", List.of("MEMBER"))))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId",
            is("00000000-0000-0000-0000-000000000001")))
        .andExpect(jsonPath("$.username", is("testuser")));
  }

  @Test
  void currentUserContextExtractsRoles() throws Exception {
    mockMvc.perform(get("/api/auth-test/roles")
            .with(jwt().jwt(builder -> builder
                .subject("00000000-0000-0000-0000-000000000002")
                .claim("preferred_username", "testadmin")
                .claim("realm_access",
                    Map.of("roles", List.of("ADMIN", "MEMBER"))))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0]", is("ADMIN")))
        .andExpect(jsonPath("$[1]", is("MEMBER")));
  }

  @Test
  void healthEndpointIsPublic() throws Exception {
    mockMvc.perform(get("/actuator/health"))
        .andExpect(status().isOk());
  }

  @Test
  void corsAllowsLocalhostOrigin() throws Exception {
    mockMvc.perform(options("/api/auth-test/me")
            .header("Origin", "http://localhost:4200")
            .header("Access-Control-Request-Method", "GET")
            .header("Access-Control-Request-Headers", "Authorization"))
        .andExpect(status().isOk())
        .andExpect(header().string(
            "Access-Control-Allow-Origin", "http://localhost:4200"));
  }

  @Test
  void corsRejectsUnknownOrigin() throws Exception {
    mockMvc.perform(options("/api/auth-test/me")
            .header("Origin", "http://evil.com")
            .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isForbidden());
  }
}
