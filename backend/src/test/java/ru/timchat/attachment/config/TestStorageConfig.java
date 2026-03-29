package ru.timchat.attachment.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import ru.timchat.attachment.infra.S3StorageClient;

@TestConfiguration
public class TestStorageConfig {

  @Bean
  @Primary
  public S3StorageClient s3StorageClient() {
    var mock = mock(S3StorageClient.class);
    when(mock.generatePresignedUploadUrl(anyString(), anyString()))
        .thenReturn("https://minio-test/upload-url");
    when(mock.generatePresignedDownloadUrl(anyString()))
        .thenReturn("https://minio-test/download-url");
    return mock;
  }
}
