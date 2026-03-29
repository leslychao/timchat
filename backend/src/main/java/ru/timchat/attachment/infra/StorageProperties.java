package ru.timchat.attachment.infra;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

  private String endpoint;
  private String accessKey;
  private String secretKey;
  private String bucket;
  private String region = "us-east-1";
  private int uploadUrlExpirySeconds = 600;
  private int downloadUrlExpirySeconds = 3600;
  private long maxFileSizeBytes = 26_214_400;
  private List<String> allowedContentTypes = List.of();
}
