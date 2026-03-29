package ru.timchat.channel.api;

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
class ChannelControllerTest {

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

  private String createWorkspaceAndGetId(String slug) throws Exception {
    var result = mockMvc.perform(post("/api/workspaces")
            .with(jwtUser())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name": "Channel WS", "slug": "%s"}
                """.formatted(slug)))
        .andExpect(status().isCreated())
        .andReturn();
    var body = result.getResponse().getContentAsString();
    return objectMapper.readTree(body).get("id").asText();
  }

  @Test
  void createChannel_textType_returns201() throws Exception {
    var wsId = createWorkspaceAndGetId("ch-ws-text");

    mockMvc.perform(post("/api/workspaces/" + wsId + "/channels")
            .with(jwtUser())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name": "general", "type": "TEXT"}
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name", is("general")))
        .andExpect(jsonPath("$.type", is("TEXT")))
        .andExpect(jsonPath("$.id", notNullValue()))
        .andExpect(jsonPath("$.position", is(0)));
  }

  @Test
  void createChannel_voiceType_returns201() throws Exception {
    var wsId = createWorkspaceAndGetId("ch-ws-voice");

    mockMvc.perform(post("/api/workspaces/" + wsId + "/channels")
            .with(jwtUser())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name": "voice-room", "type": "VOICE"}
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type", is("VOICE")));
  }

  @Test
  void createChannel_videoType_returns201() throws Exception {
    var wsId = createWorkspaceAndGetId("ch-ws-video");

    mockMvc.perform(post("/api/workspaces/" + wsId + "/channels")
            .with(jwtUser())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name": "video-room", "type": "VIDEO"}
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type", is("VIDEO")));
  }

  @Test
  void createChannel_invalidType_returns400() throws Exception {
    var wsId = createWorkspaceAndGetId("ch-ws-invalid-type");

    mockMvc.perform(post("/api/workspaces/" + wsId + "/channels")
            .with(jwtUser())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name": "bad", "type": "INVALID"}
                """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void listChannels_afterCreate_returnsChannel() throws Exception {
    var wsId = createWorkspaceAndGetId("ch-ws-list");

    mockMvc.perform(post("/api/workspaces/" + wsId + "/channels")
            .with(jwtUser())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name": "listed", "type": "TEXT"}
                """))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/api/workspaces/" + wsId + "/channels")
            .with(jwtUser()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  void updateChannel_changesName_keepType() throws Exception {
    var wsId = createWorkspaceAndGetId("ch-ws-update");

    var createResult = mockMvc.perform(
            post("/api/workspaces/" + wsId + "/channels")
                .with(jwtUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "old-name", "type": "TEXT"}
                    """))
        .andExpect(status().isCreated())
        .andReturn();

    var channelId = objectMapper.readTree(
        createResult.getResponse().getContentAsString())
        .get("id").asText();

    mockMvc.perform(put("/api/channels/" + channelId)
            .with(jwtUser())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name": "new-name"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("new-name")))
        .andExpect(jsonPath("$.type", is("TEXT")));
  }

  @Test
  void deleteChannel_returns204() throws Exception {
    var wsId = createWorkspaceAndGetId("ch-ws-delete");

    var createResult = mockMvc.perform(
            post("/api/workspaces/" + wsId + "/channels")
                .with(jwtUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "to-delete", "type": "TEXT"}
                    """))
        .andExpect(status().isCreated())
        .andReturn();

    var channelId = objectMapper.readTree(
        createResult.getResponse().getContentAsString())
        .get("id").asText();

    mockMvc.perform(delete("/api/channels/" + channelId)
            .with(jwtUser()))
        .andExpect(status().isNoContent());
  }

  @Test
  void createChannel_positionIncrementsAutomatically() throws Exception {
    var wsId = createWorkspaceAndGetId("ch-ws-position");

    mockMvc.perform(post("/api/workspaces/" + wsId + "/channels")
            .with(jwtUser())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name": "first", "type": "TEXT"}
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.position", is(0)));

    mockMvc.perform(post("/api/workspaces/" + wsId + "/channels")
            .with(jwtUser())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name": "second", "type": "VOICE"}
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.position", is(1)));
  }
}
