package com.cloudimny.api.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.net.URI;
import java.nio.ByteBuffer;

import static com.cloudimny.api.config.StorageConfig.COVERS_BUCKET_NAME;
import static com.cloudimny.api.config.StorageConfig.TRACKS_BUCKET_NAME;

@Service
@Slf4j
public class StorageService {
    private final S3AsyncClient s3Client;
    private final DataBufferFactory dataBufferFactory;
    private final WebClient webClient;

    public StorageService(S3AsyncClient s3Client,
                          DataBufferFactory dataBufferFactory,
                          ReactorClientHttpConnector externalHttpConnector) {
        this.s3Client = s3Client;
        this.dataBufferFactory = dataBufferFactory;
        this.webClient = WebClient.builder()
                .clientConnector(externalHttpConnector)
                .build();
    }

    public Mono<Void> deleteTrack(String key) {
        return deleteByKeyAndBucket(key, TRACKS_BUCKET_NAME);
    }

    public Mono<Void> deleteCover(String key) {
        return deleteByKeyAndBucket(key, COVERS_BUCKET_NAME);
    }

    private Mono<Void> deleteByKeyAndBucket(String key, String bucket) {
        var request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        return Mono.fromFuture(s3Client.deleteObject(request)).then();
    }

    public Mono<ResponseEntity<Flux<DataBuffer>>> loadTrack(String key, String range) {
        var requestBuilder = GetObjectRequest.builder()
                .bucket(TRACKS_BUCKET_NAME)
                .key(key);

        if (range != null) {
            requestBuilder.range(range);
        }

        var getObject = s3Client.getObject(
                requestBuilder.build(),
                AsyncResponseTransformer.toPublisher()
        );

        return Mono.fromFuture(getObject)
                .map(publisher -> {
                    GetObjectResponse metadata = publisher.response();
                    Flux<DataBuffer> body = Flux.from(publisher).map(this::toDataBuffer);

                    HttpStatus status = metadata.contentRange() != null
                            ? HttpStatus.PARTIAL_CONTENT
                            : HttpStatus.OK;

                    var responseBuilder = ResponseEntity.status(status)
                            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                            .contentLength(metadata.contentLength());

                    if (metadata.contentType() != null) {
                        responseBuilder.contentType(MediaType.parseMediaType(metadata.contentType()));
                    }

                    if (metadata.contentRange() != null) {
                        responseBuilder.header(HttpHeaders.CONTENT_RANGE, metadata.contentRange());
                    }

                    return responseBuilder.body(body);
                });
    }

    public Mono<Void> uploadTrack(String key, FilePart file) {
        return DataBufferUtils.join(file.content())
                .flatMap(dataBuffer -> {
                    ByteBuffer buffer = toByteBuffer(dataBuffer);

                    var request = PutObjectRequest.builder()
                            .key(key)
                            .bucket(TRACKS_BUCKET_NAME)
                            .contentType(contentTypeOf(file))
                            .contentLength((long) buffer.remaining())
                            .build();

                    var putObject = s3Client.putObject(request, AsyncRequestBody.fromByteBuffer(buffer));
                    return Mono.fromFuture(putObject);
                })
                .then();
    }

    public Mono<Void> downloadCover(String key, URI source) {
        log.info("Downloading cover from {}", source);

        return webClient
                .get()
                .uri(source)
                .retrieve()
                .toEntity(byte[].class)
                .flatMap(response -> {
                    byte[] body = response.getBody();
                    if (body == null || body.length == 0) {
                        return Mono.empty();
                    }

                    var putRequest = PutObjectRequest.builder()
                            .key(key)
                            .bucket(COVERS_BUCKET_NAME)
                            .contentType(contentTypeOf(response.getHeaders()))
                            .contentLength((long) body.length)
                            .build();

                    var putResponse = s3Client.putObject(putRequest,
                            AsyncRequestBody.fromBytes(body));

                    return Mono.fromFuture(putResponse);
                }).then();
    }

    public Mono<ResponseEntity<Flux<DataBuffer>>> loadCover(String key) {
        var getRequest = GetObjectRequest.builder()
                .key(key)
                .bucket(COVERS_BUCKET_NAME)
                .build();

        var getObject = s3Client.getObject(getRequest, AsyncResponseTransformer.toPublisher());

        return Mono.fromFuture(getObject)
                .map(publisher -> {
                    Flux<DataBuffer> body = Flux.from(publisher).map(this::toDataBuffer);
                    return ResponseEntity.ok(body);
                });
    }

    private String contentTypeOf(FilePart file) {
        MediaType contentType = file.headers().getContentType();
        return contentType != null ? contentType.toString() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private String contentTypeOf(HttpHeaders headers) {
        MediaType contentType = headers.getContentType();
        return contentType != null ? contentType.toString() : MediaType.IMAGE_JPEG_VALUE;
    }

    private ByteBuffer toByteBuffer(DataBuffer dataBuffer) {
        try (DataBuffer.ByteBufferIterator iterator = dataBuffer.readableByteBuffers()) {
            ByteBuffer copy = ByteBuffer.allocate(dataBuffer.readableByteCount());
            while (iterator.hasNext()) {
                copy.put(iterator.next());
            }
            copy.flip();
            return copy;
        } finally {
            DataBufferUtils.release(dataBuffer);
        }
    }

    private DataBuffer toDataBuffer(ByteBuffer byteBuffer) {
        return dataBufferFactory.wrap(byteBuffer);
    }
}
