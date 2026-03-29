package ru.timchat.attachment.api;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.timchat.attachment.config.TestStorageConfig;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestStorageConfig.class)
class AttachmentControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  private static final String USER_ID =
      "00000000-0000-0000-0000-000000000050";
  private static final String OTHER_USER_ID =
      "00000000-0000-0000-0000-000000000051";

  private static JwtRequestPostProcessor jwtUser() {
    return jwt().jwt(builder -> builder
        .subject(USER_ID)
        .claim("preferred_username", "attachuser")
        .claim("email", "attachuser@test.com")
        .claim("realm_access",
            Map.of("roles", List.of("MEMBER"))));
  }

  private static JwtRequestPostProcessor jwtOtherUser() {
    return jwt().jwt(builder -> builder
        .subject(OTHER_USER_ID)
        .claim("preferred_username", "otherattachuser")
        .claim("email", "otherattach@test.com")
        .claim("realm_access",
            Map.of("roles", List.of("MEMBER"))));
  }

  private String createWorkspaceAndGetId(String slug)
      throws Exception {
    var result = mockMvc.perform(post("/api/workspaces")
            .with(jwtUser())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name": "Attach WS", "slug": "%s"}
                """.formatted(slug)))
        .andExpect(status().isCreated())
        .andReturn();
    return objectMapper.readTree(
        result.getResponse().getContentAsString())
        .get("id").asText();
  }

  @Test
  void initiateUpload_validRequest_returns201() throws Exception {
    var wsId = createWorkspaceAndGetId("attach-ws-upload");

    mockMvc.perform(post("/api/workspaces/" + wsId
            + "/attachments/upload")
            .with(jwtUser())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "fileName": "photo.jpg",
                  "contentType": "image/jpeg",
                  "sizeBytes": 1024
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.attachmentId", notNullValue()))
        .andExpect(jsonPath("$.uploadUrl", notNullValue()));
  }

  @Test
  void initiateUpload_invalidContentType_returns422()
      throws Exception {
    var wsId = createWorkspaceAndGetId("attach-ws-badtype");

    mockMvc.perform(post("/api/workspaces/" + wsId
            + "/attachments/upload")
            .with(jwtUser())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "fileName": "virus.exe",
                  "contentType": "application/x-msdownload",
                  "sizeBytes": 1024
                }
                """))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
  }

  @Test
  void initiateUpload_exceedsSize_returns422() throws Exception {
    var wsId = createWorkspaceAndGetId("attach-ws-bigfile");

    mockMvc.perform(post("/api/workspaces/" + wsId
            + "/attachments/upload")
            .with(jwtUser())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "fileName": "huge.pdf",
                  "contentType": "application/pdf",
                  "sizeBytes": 100000000
                }
                """))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
  }

  @Test
  void confirmUpload_validOwner_returns200() throws Exception {
    var wsId = createWorkspaceAndGetId("attach-ws-confirm");

    var uploadResult = mockMvc.perform(
            post("/api/workspaces/" + wsId + "/attachments/upload")
                .with(jwtUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fileName": "doc.pdf",
                      "contentType": "application/pdf",
                      "sizeBytes": 2048
                    }
                    """))
        .andExpect(status().isCreated())
        .andReturn();

    var attachmentId = objectMapper.readTree(
        uploadResult.getResponse().getContentAsString())
        .get("attachmentId").asText();

    mockMvc.perform(post("/api/attachments/" + attachmentId
            + "/confirm")
            .with(jwtUser()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(attachmentId)))
        .andExpect(jsonPath("$.status", is("UPLOADED")))
        .andExpect(jsonPath("$.fileName", is("doc.pdf")));
  }

  @Test
  void getDownloadUrl_afterConfirm_returnsUrl() throws Exception {
    var wsId = createWorkspaceAndGetId("attach-ws-download");

    var uploadResult = mockMvc.perform(
            post("/api/workspaces/" + wsId + "/attachments/upload")
                .with(jwtUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fileName": "report.pdf",
                      "contentType": "application/pdf",
                      "sizeBytes": 4096
                    }
                    """))
        .andExpect(status().isCreated())
        .andReturn();

    var attachmentId = objectMapper.readTree(
        uploadResult.getResponse().getContentAsString())
        .get("attachmentId").asText();

    mockMvc.perform(post("/api/attachments/" + attachmentId
            + "/confirm")
            .with(jwtUser()))
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/attachments/" + attachmentId
            + "/download-url")
            .with(jwtUser()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.attachmentId", is(attachmentId)))
        .andExpect(jsonPath("$.downloadUrl", notNullValue()))
        .andExpect(jsonPath("$.fileName", is("report.pdf")))
        .andExpect(jsonPath("$.contentType",
            is("application/pdf")));
  }

  @Test
  void getDownloadUrl_notUploaded_returns422() throws Exception {
    var wsId = createWorkspaceAndGetId("attach-ws-notup");

    var uploadResult = mockMvc.perform(
            post("/api/workspaces/" + wsId + "/attachments/upload")
                .with(jwtUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fileName": "pending.pdf",
                      "contentType": "application/pdf",
                      "sizeBytes": 512
                    }
                    """))
        .andExpect(status().isCreated())
        .andReturn();

    var attachmentId = objectMapper.readTree(
        uploadResult.getResponse().getContentAsString())
        .get("attachmentId").asText();

    mockMvc.perform(get("/api/attachments/" + attachmentId
            + "/download-url")
            .with(jwtUser()))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void initiateUpload_missingFileName_returns422() throws Exception {
    var wsId = createWorkspaceAndGetId("attach-ws-noname");

    mockMvc.perform(post("/api/workspaces/" + wsId
            + "/attachments/upload")
            .with(jwtUser())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "contentType": "image/jpeg",
                  "sizeBytes": 1024
                }
                """))
        .andExpect(status().isUnprocessableEntity());
  }
}
