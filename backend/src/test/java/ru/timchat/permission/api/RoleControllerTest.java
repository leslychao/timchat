package ru.timchat.permission.api;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoleControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  private static final String USER_ID =
      "00000000-0000-0000-0000-000000000001";

  private static JwtRequestPostProcessor jwtUser() {
    return jwt().jwt(builder -> builder
        .subject(USER_ID)
        .claim("preferred_username", "testuser")
        .claim("email", "test@test.com")
        .claim("realm_access",
            Map.of("roles", List.of("MEMBER"))));
  }

  @Test
  void createWorkspace_seedsDefaultRoles() throws Exception {
    var wsId = createWorkspace("roles-ws-1");

    mockMvc.perform(get("/api/workspaces/" + wsId + "/roles")
            .with(jwtUser()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(4)))
        .andExpect(jsonPath("$[*].name",
            everyItem(is(in(List.of(
                "OWNER", "ADMIN", "MEMBER", "GUEST"))))));
  }

  @Test
  void ownerHasOwnerRole_afterWorkspaceCreation() throws Exception {
    var wsId = createWorkspace("roles-ws-2");
    var memberId = getFirstMemberId(wsId);

    mockMvc.perform(get("/api/workspaces/" + wsId
                + "/members/" + memberId + "/roles")
            .with(jwtUser()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].name", is("OWNER")));
  }

  @Test
  void assignRole_returns201() throws Exception {
    var wsId = createWorkspace("roles-ws-3");
    var memberId = getFirstMemberId(wsId);

    mockMvc.perform(post("/api/workspaces/" + wsId
                + "/members/" + memberId + "/roles")
            .with(jwtUser())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"roleName": "ADMIN"}
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name", is("ADMIN")))
        .andExpect(jsonPath("$.id", notNullValue()));
  }

  @Test
  void assignRole_duplicate_returns409() throws Exception {
    var wsId = createWorkspace("roles-ws-4");
    var memberId = getFirstMemberId(wsId);

    mockMvc.perform(post("/api/workspaces/" + wsId
                + "/members/" + memberId + "/roles")
            .with(jwtUser())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"roleName": "OWNER"}
                """))
        .andExpect(status().isConflict());
  }

  @Test
  void revokeRole_returns204() throws Exception {
    var wsId = createWorkspace("roles-ws-5");
    var memberId = getFirstMemberId(wsId);

    var assignResult = mockMvc.perform(post("/api/workspaces/" + wsId
                + "/members/" + memberId + "/roles")
            .with(jwtUser())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"roleName": "ADMIN"}
                """))
        .andExpect(status().isCreated())
        .andReturn();

    var roleNode = objectMapper.readTree(
        assignResult.getResponse().getContentAsString());
    var roleId = roleNode.get("id").asText();

    mockMvc.perform(delete("/api/workspaces/" + wsId
                + "/members/" + memberId + "/roles/" + roleId)
            .with(jwtUser()))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/workspaces/" + wsId
                + "/members/" + memberId + "/roles")
            .with(jwtUser()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.name == 'ADMIN')]").doesNotExist());
  }

  private String createWorkspace(String slug) throws Exception {
    var result = mockMvc.perform(post("/api/workspaces")
            .with(jwtUser())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\": \"WS\", \"slug\": \"" + slug + "\"}"))
        .andExpect(status().isCreated())
        .andReturn();

    var node = objectMapper.readTree(
        result.getResponse().getContentAsString());
    return node.get("id").asText();
  }

  private String getFirstMemberId(String wsId) throws Exception {
    var result = mockMvc.perform(
            get("/api/workspaces/" + wsId + "/members")
                .with(jwtUser()))
        .andExpect(status().isOk())
        .andReturn();

    var nodes = objectMapper.readTree(
        result.getResponse().getContentAsString());
    return nodes.get(0).get("id").asText();
  }
}
