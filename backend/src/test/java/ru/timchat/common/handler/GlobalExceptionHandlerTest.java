package ru.timchat.common.handler;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.timchat.common.error.ConflictException;
import ru.timchat.common.error.ForbiddenException;
import ru.timchat.common.error.NotFoundException;
import ru.timchat.common.error.ValidationException;

class GlobalExceptionHandlerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    MessageSource messageSource = createMessageSource();
    GlobalExceptionHandler handler = new GlobalExceptionHandler(messageSource);

    mockMvc = MockMvcBuilders
        .standaloneSetup(new TestController())
        .setControllerAdvice(handler)
        .build();
  }

  @Test
  void notFoundReturns404WithEnglishMessage() throws Exception {
    mockMvc.perform(get("/test/not-found")
            .locale(Locale.ENGLISH))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code", is("NOT_FOUND")))
        .andExpect(jsonPath("$.message", is("Resource not found")))
        .andExpect(jsonPath("$.traceId", notNullValue()));
  }

  @Test
  void notFoundReturns404WithRussianMessage() throws Exception {
    mockMvc.perform(get("/test/not-found")
            .locale(new Locale("ru")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code", is("NOT_FOUND")))
        .andExpect(jsonPath("$.message",
            is("\u0420\u0435\u0441\u0443\u0440\u0441 \u043d\u0435 "
                + "\u043d\u0430\u0439\u0434\u0435\u043d")))
        .andExpect(jsonPath("$.traceId", notNullValue()));
  }

  @Test
  void missingLocaleDefaultsToEnglish() throws Exception {
    mockMvc.perform(get("/test/not-found"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code", is("NOT_FOUND")))
        .andExpect(jsonPath("$.message", is("Resource not found")));
  }

  @Test
  void forbiddenReturns403() throws Exception {
    mockMvc.perform(get("/test/forbidden")
            .locale(Locale.ENGLISH))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code", is("FORBIDDEN")))
        .andExpect(jsonPath("$.message", is("Access denied")))
        .andExpect(jsonPath("$.traceId", notNullValue()));
  }

  @Test
  void conflictReturns409() throws Exception {
    mockMvc.perform(get("/test/conflict")
            .locale(Locale.ENGLISH))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code", is("CONFLICT")))
        .andExpect(jsonPath("$.message", is("Resource conflict")))
        .andExpect(jsonPath("$.traceId", notNullValue()));
  }

  @Test
  void validationReturns422() throws Exception {
    mockMvc.perform(get("/test/validation")
            .locale(Locale.ENGLISH))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
        .andExpect(jsonPath("$.message", is("Validation error")))
        .andExpect(jsonPath("$.traceId", notNullValue()));
  }

  @Test
  void genericExceptionReturns500() throws Exception {
    mockMvc.perform(get("/test/internal")
            .locale(Locale.ENGLISH))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code", is("INTERNAL_ERROR")))
        .andExpect(jsonPath("$.message", is("Internal server error")))
        .andExpect(jsonPath("$.details", nullValue()))
        .andExpect(jsonPath("$.traceId", notNullValue()));
  }

  @Test
  void forbiddenReturnsRussianMessage() throws Exception {
    mockMvc.perform(get("/test/forbidden")
            .locale(new Locale("ru")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message",
            is("\u0414\u043e\u0441\u0442\u0443\u043f "
                + "\u0437\u0430\u043f\u0440\u0435\u0449\u0435\u043d")));
  }

  @Test
  void unsupportedLocaleDefaultsToEnglish() throws Exception {
    mockMvc.perform(get("/test/not-found")
            .locale(Locale.FRENCH))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message", is("Resource not found")));
  }

  private MessageSource createMessageSource() {
    var source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:messages");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(true);
    source.setFallbackToSystemLocale(false);
    return source;
  }

  @RestController
  static class TestController {

    @GetMapping("/test/not-found")
    @ResponseStatus(HttpStatus.OK)
    public void throwNotFound() {
      throw new NotFoundException("error.not-found");
    }

    @GetMapping("/test/forbidden")
    @ResponseStatus(HttpStatus.OK)
    public void throwForbidden() {
      throw new ForbiddenException("error.forbidden");
    }

    @GetMapping("/test/conflict")
    @ResponseStatus(HttpStatus.OK)
    public void throwConflict() {
      throw new ConflictException("error.conflict");
    }

    @GetMapping("/test/validation")
    @ResponseStatus(HttpStatus.OK)
    public void throwValidation() {
      throw new ValidationException("error.validation");
    }

    @GetMapping("/test/internal")
    @ResponseStatus(HttpStatus.OK)
    public void throwInternal() {
      throw new RuntimeException("unexpected failure");
    }
  }
}
