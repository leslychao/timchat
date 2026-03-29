package ru.timchat.workspace.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class WorkspaceControllerTest {

  @Autowired
  private MockMvc mockMvc;

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
  void createWorkspace_returns201() throws Exception {
    mockMvc.perform(post("/api/workspaces")
            .with(jwtUser())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name": "Test WS", "slug": "test-ws-1"}
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name", is("Test WS")))
        .andExpect(jsonPath("$.slug", is("test-ws-1")))
        .andExpect(jsonPath("$.id", notNullValue()));
  }

  @Test
  void listWorkspaces_afterCreate_returnsWorkspace() throws Exception {
    mockMvc.perform(post("/api/workspaces")
            .with(jwtUser())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name": "List WS", "slug": "list-ws"}
                """))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/api/workspaces")
            .with(jwtUser()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[?(@.slug == 'list-ws')]").exists());
  }

  @Test
  void listMembers_afterCreate_containsOwner() throws Exception {
    var profileResult = mockMvc.perform(get("/api/users/me")
            .with(jwtUser()))
        .andExpect(status().isOk())
        .andReturn();
    var profileBody = profileResult.getResponse().getContentAsString();
    var uidStart = profileBody.indexOf("\"id\":\"") + 6;
    var uidEnd = profileBody.indexOf("\"", uidStart);
    var internalUserId = profileBody.substring(uidStart, uidEnd);

    var createResult = mockMvc.perform(post("/api/workspaces")
            .with(jwtUser())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name": "Members WS", "slug": "members-ws"}
                """))
        .andExpect(status().isCreated())
        .andReturn();

    var body = createResult.getResponse().getContentAsString();
    var idStart = body.indexOf("\"id\":\"") + 6;
    var idEnd = body.indexOf("\"", idStart);
    var workspaceId = body.substring(idStart, idEnd);

    mockMvc.perform(
            get("/api/workspaces/" + workspaceId + "/members")
                .with(jwtUser()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].userId", is(internalUserId)));
  }

  @Test
  void createWorkspace_duplicateSlug_returns409() throws Exception {
    mockMvc.perform(post("/api/workspaces")
            .with(jwtUser())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name": "Dup WS", "slug": "dup-slug-ws"}
                """))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/workspaces")
            .with(jwtUser())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name": "Dup WS 2", "slug": "dup-slug-ws"}
                """))
        .andExpect(status().isConflict());
  }

  @Test
  void getUserProfile_returnsCurrentUser() throws Exception {
    mockMvc.perform(get("/api/users/me")
            .with(jwtUser()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username", is("testuser")))
        .andExpect(jsonPath("$.id", notNullValue()));
  }

  @Test
  void updateProfile_changesDisplayName() throws Exception {
    mockMvc.perform(get("/api/users/me")
        .with(jwtUser()));

    mockMvc.perform(put("/api/users/me/profile")
            .with(jwtUser())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"displayName": "New Display Name"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName",
            is("New Display Name")));
  }
}
