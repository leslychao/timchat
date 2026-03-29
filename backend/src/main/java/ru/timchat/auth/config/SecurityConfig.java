package ru.timchat.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import ru.timchat.common.error.ErrorResponse;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final ObjectMapper objectMapper;
  private final MessageSource messageSource;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http)
      throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(sm ->
            sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health").permitAll()
            .requestMatchers("/v3/api-docs/**").permitAll()
            .requestMatchers("/swagger-ui/**").permitAll()
            .anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(Customizer.withDefaults())
            .authenticationEntryPoint(authenticationEntryPoint())
            .accessDeniedHandler(accessDeniedHandler()));
    return http.build();
  }

  private AuthenticationEntryPoint authenticationEntryPoint() {
    return (request, response, authException) -> {
      var locale = request.getLocale();
      var message = messageSource.getMessage(
          "error.auth.unauthorized", null, "Authentication required", locale);
      var traceId = UUID.randomUUID().toString();

      log.warn("Authentication failed [traceId={}]: {}",
          traceId, authException.getMessage());

      response.setStatus(HttpStatus.UNAUTHORIZED.value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      objectMapper.writeValue(response.getOutputStream(),
          new ErrorResponse("UNAUTHORIZED", message, null, traceId));
    };
  }

  private AccessDeniedHandler accessDeniedHandler() {
    return (request, response, accessDeniedException) -> {
      var locale = request.getLocale();
      var message = messageSource.getMessage(
          "error.auth.forbidden", null, "Access denied", locale);
      var traceId = UUID.randomUUID().toString();

      log.warn("Access denied [traceId={}]: {}",
          traceId, accessDeniedException.getMessage());

      response.setStatus(HttpStatus.FORBIDDEN.value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      objectMapper.writeValue(response.getOutputStream(),
          new ErrorResponse("FORBIDDEN", message, null, traceId));
    };
  }
}
