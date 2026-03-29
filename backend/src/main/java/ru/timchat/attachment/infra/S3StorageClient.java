package ru.timchat.attachment.infra;

import java.net.URI;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Component
public class S3StorageClient {

  private final S3Presigner presigner;
  private final S3Client s3Client;
  private final StorageProperties properties;

  public S3StorageClient(StorageProperties properties) {
    this.properties = properties;

    var credentials = StaticCredentialsProvider.create(
        AwsBasicCredentials.create(
            properties.getAccessKey(), properties.getSecretKey()));

    this.s3Client = S3Client.builder()
        .endpointOverride(URI.create(properties.getEndpoint()))
        .credentialsProvider(credentials)
        .region(Region.of(properties.getRegion()))
        .forcePathStyle(true)
        .build();

    this.presigner = S3Presigner.builder()
        .endpointOverride(URI.create(properties.getEndpoint()))
        .credentialsProvider(credentials)
        .region(Region.of(properties.getRegion()))
        .build();

    ensureBucketExists();
  }

  public String generatePresignedUploadUrl(String storageKey,
      String contentType) {
    var putObjectRequest = PutObjectRequest.builder()
        .bucket(properties.getBucket())
        .key(storageKey)
        .contentType(contentType)
        .build();

    var presignRequest = PutObjectPresignRequest.builder()
        .signatureDuration(Duration.ofSeconds(
            properties.getUploadUrlExpirySeconds()))
        .putObjectRequest(putObjectRequest)
        .build();

    var presignedRequest = presigner.presignPutObject(presignRequest);
    return presignedRequest.url().toString();
  }

  public String generatePresignedDownloadUrl(String storageKey) {
    var getObjectRequest = GetObjectRequest.builder()
        .bucket(properties.getBucket())
        .key(storageKey)
        .build();

    var presignRequest = GetObjectPresignRequest.builder()
        .signatureDuration(Duration.ofSeconds(
            properties.getDownloadUrlExpirySeconds()))
        .getObjectRequest(getObjectRequest)
        .build();

    var presignedRequest = presigner.presignGetObject(presignRequest);
    return presignedRequest.url().toString();
  }

  private void ensureBucketExists() {
    try {
      s3Client.headBucket(HeadBucketRequest.builder()
          .bucket(properties.getBucket())
          .build());
      log.info("S3 bucket exists: {}", properties.getBucket());
    } catch (NoSuchBucketException e) {
      s3Client.createBucket(CreateBucketRequest.builder()
          .bucket(properties.getBucket())
          .build());
      log.info("S3 bucket created: {}", properties.getBucket());
    } catch (Exception e) {
      log.warn("Could not verify/create S3 bucket '{}': {}",
          properties.getBucket(), e.getMessage());
    }
  }
}
