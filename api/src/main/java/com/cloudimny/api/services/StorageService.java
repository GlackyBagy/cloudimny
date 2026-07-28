package com.cloudimny.api.services;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.ByteBuffer;

import static com.cloudimny.api.config.StorageConfig.BUCKET_NAME;

@Service
@RequiredArgsConstructor
public class StorageService {
    private final S3AsyncClient s3Client;
    private final DataBufferFactory dataBufferFactory;

    public Mono<ResponseEntity<Flux<DataBuffer>>> load(String key, String range) {
        var requestBuilder = GetObjectRequest.builder()
                .bucket(BUCKET_NAME)
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

    public Mono<Void> upload(String key, FilePart file) {
        String contentType = MediaType.MULTIPART_FORM_DATA_VALUE;

        var request = PutObjectRequest.builder()
                .key(key)
                .bucket(BUCKET_NAME)
                .contentType(contentType)
                .build();

        var putObject = s3Client.putObject(
                request,
                AsyncRequestBody.fromPublisher(file.content().map(this::toByteBuffer))
        );

        return Mono.fromFuture(putObject).then();
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
