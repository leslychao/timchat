package ru.timchat.message.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
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
class MessageControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  private static final String USER_ID =
      "00000000-0000-0000-0000-000000000099";
  private static final String OTHER_USER_ID =
      "00000000-0000-0000-0000-000000000098";

  private static JwtRequestPostProcessor jwtUser() {
    return jwt().jwt(builder -> builder
        .subject(USER_ID)
        .claim("preferred_username", "msguser")
        .claim("email", "msguser@test.com")
        .claim("realm_access",
            Map.of("roles", List.of("MEMBER"))));
  }

  private static JwtRequestPostProcessor jwtOtherUser() {
    return jwt().jwt(builder -> builder
        .subject(OTHER_USER_ID)
        .claim("preferred_username", "otheruser")
        .claim("email", "other@test.com")
        .claim("realm_access",
            Map.of("roles", List.of("MEMBER"))));
  }

  private String createWorkspaceAndGetId(String slug) throws Exception {
    var result = mockMvc.perform(post("/api/workspaces")
            .with(jwtUser())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name": "Msg WS", "slug": "%s"}
                """.formatted(slug)))
        .andExpect(status().isCreated())
        .andReturn();
    return objectMapper.readTree(
        result.getResponse().getContentAsString())
        .get("id").asText();
  }

  private String createChannelAndGetId(String wsId) throws Exception {
    var result = mockMvc.perform(
            post("/api/workspaces/" + wsId + "/channels")
                .with(jwtUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "general", "type": "TEXT"}
                    """))
        .andExpect(status().isCreated())
        .andReturn();
    return objectMapper.readTree(
        result.getResponse().getContentAsString())
        .get("id").asText();
  }

  @Test
  void sendMessage_returns201() throws Exception {
    var wsId = createWorkspaceAndGetId("msg-ws-send");
    var chId = createChannelAndGetId(wsId);

    mockMvc.perform(post("/api/channels/" + chId + "/messages")
            .with(jwtUser())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"content": "Hello world"}
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.content", is("Hello world")))
        .andExpect(jsonPath("$.channelId", is(chId)))
        .andExpect(jsonPath("$.authorId", notNullValue()))
        .andExpect(jsonPath("$.deleted", is(false)))
        .andExpect(jsonPath("$.id", notNullValue()));
  }

  @Test
  void sendMessage_emptyContent_returns422() throws Exception {
    var wsId = createWorkspaceAndGetId("msg-ws-empty");
    var chId = createChannelAndGetId(wsId);

    mockMvc.perform(post("/api/channels/" + chId + "/messages")
            .with(jwtUser())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"content": ""}
                """))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void editMessage_changesContent() throws Exception {
    var wsId = createWorkspaceAndGetId("msg-ws-edit");
    var chId = createChannelAndGetId(wsId);

    var sendResult = mockMvc.perform(
            post("/api/channels/" + chId + "/messages")
                .with(jwtUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"content": "Original"}
                    """))
        .andExpect(status().isCreated())
        .andReturn();

    var msgId = objectMapper.readTree(
        sendResult.getResponse().getContentAsString())
        .get("id").asText();

    mockMvc.perform(put("/api/messages/" + msgId)
            .with(jwtUser())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"content": "Updated"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", is("Updated")))
        .andExpect(jsonPath("$.id", is(msgId)));
  }

  @Test
  void deleteMessage_returns204() throws Exception {
    var wsId = createWorkspaceAndGetId("msg-ws-del");
    var chId = createChannelAndGetId(wsId);

    var sendResult = mockMvc.perform(
            post("/api/channels/" + chId + "/messages")
                .with(jwtUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"content": "To delete"}
                    """))
        .andExpect(status().isCreated())
        .andReturn();

    var msgId = objectMapper.readTree(
        sendResult.getResponse().getContentAsString())
        .get("id").asText();

    mockMvc.perform(delete("/api/messages/" + msgId)
            .with(jwtUser()))
        .andExpect(status().isNoContent());
  }

  @Test
  void getHistory_afterSend_returnsMessage() throws Exception {
    var wsId = createWorkspaceAndGetId("msg-ws-hist");
    var chId = createChannelAndGetId(wsId);

    mockMvc.perform(post("/api/channels/" + chId + "/messages")
            .with(jwtUser())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"content": "History msg"}
                """))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/api/channels/" + chId + "/messages")
            .with(jwtUser()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].content", is("History msg")))
        .andExpect(jsonPath("$.hasMore", is(false)));
  }

  @Test
  void getHistory_deletedMessages_notReturned() throws Exception {
    var wsId = createWorkspaceAndGetId("msg-ws-hist-del");
    var chId = createChannelAndGetId(wsId);

    var sendResult = mockMvc.perform(
            post("/api/channels/" + chId + "/messages")
                .with(jwtUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"content": "Will be deleted"}
                    """))
        .andExpect(status().isCreated())
        .andReturn();

    var msgId = objectMapper.readTree(
        sendResult.getResponse().getContentAsString())
        .get("id").asText();

    mockMvc.perform(delete("/api/messages/" + msgId)
            .with(jwtUser()))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/channels/" + chId + "/messages")
            .with(jwtUser()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(0)));
  }

  @Test
  void getHistory_withPagination_returnsPaginatedResults()
      throws Exception {
    var wsId = createWorkspaceAndGetId("msg-ws-page");
    var chId = createChannelAndGetId(wsId);

    for (int i = 0; i < 3; i++) {
      mockMvc.perform(post("/api/channels/" + chId + "/messages")
              .with(jwtUser())
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  {"content": "msg-%d"}
                  """.formatted(i)))
          .andExpect(status().isCreated());
    }

    var page1Result = mockMvc.perform(
            get("/api/channels/" + chId + "/messages?limit=2")
                .with(jwtUser()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(2)))
        .andExpect(jsonPath("$.hasMore", is(true)))
        .andExpect(jsonPath("$.nextCursor", notNullValue()))
        .andReturn();

    var nextCursor = objectMapper.readTree(
        page1Result.getResponse().getContentAsString())
        .get("nextCursor").asText();

    mockMvc.perform(get("/api/channels/" + chId
            + "/messages?limit=2&cursor=" + nextCursor)
            .with(jwtUser()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.hasMore", is(false)))
        .andExpect(jsonPath("$.nextCursor", nullValue()));
  }
}
