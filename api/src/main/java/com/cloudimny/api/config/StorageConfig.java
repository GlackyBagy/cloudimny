package com.cloudimny.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import java.net.URI;
import java.util.concurrent.CompletionException;

@Configuration
public class StorageConfig {
    public static String BUCKET_NAME = "tracks";

    @Bean
    public S3AsyncClient s3Client(@Value("${storage.s3-accessKey}") String access,
                                  @Value("${storage.s3-secretKey}") String secret,
                                  @Value("${storage.s3-endpoint}") String endpoint) {
        var credentials = AwsBasicCredentials.create(access, secret);

        S3AsyncClient client = S3AsyncClient.builder()
                .region(Region.EU_CENTRAL_1)
                .forcePathStyle(true)
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(
                        StaticCredentialsProvider.create(credentials)
                )
                .build();

        createBucket(client);
        return client;
    }

    private void createBucket(S3AsyncClient client) {
        var bucket = CreateBucketRequest.builder()
                .bucket(BUCKET_NAME)
                .build();

        client.createBucket(bucket).exceptionally(throwable -> {
            Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null
                    ? throwable.getCause()
                    : throwable;

            if (cause instanceof BucketAlreadyExistsException || cause instanceof BucketAlreadyOwnedByYouException) {
                return null;
            }

            throw new RuntimeException("Failed to create storage bucket", cause);
        }).join();
    }
}
