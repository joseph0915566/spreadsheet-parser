package com.spreadsheet_parser.service.aws.s3;

import com.spreadsheet_parser.service.aws.CredentialProvider;
import com.spreadsheet_parser.service.aws.s3.model.CompleteMultipartRequest;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Service
public class S3Service {

    private final S3Client client;
    private final BucketProvider bucketProvider;

    @Inject
    public S3Service(
            BucketProvider bucketProvider,
            CredentialProvider credentialProvider
    ){
        this.bucketProvider = bucketProvider;
        this.client = S3Client
                .builder()
                .credentialsProvider(credentialProvider.get())
                .region(bucketProvider.getRegion())
                .build();
    }

    public String generateUploadId(String key){
        return client.createMultipartUpload(
                CreateMultipartUploadRequest.builder()
                        .bucket(bucketProvider.getBucketName())
                        .key(key)
                        .build()
        ).uploadId();
    }

    public String uploadPart(String uploadId, String key, int partNumber, String payload){
        return client.uploadPart(
                UploadPartRequest
                        .builder()
                        .partNumber(partNumber)
                        .uploadId(uploadId)
                        .key(key)
                        .bucket(bucketProvider.getBucketName())
                        .build(),
                RequestBody.fromString(payload, StandardCharsets.UTF_8)
        ).eTag();
    }

    public AbortMultipartUploadResponse abortMultiPartUpload(String uploadId, String key){
        return client.abortMultipartUpload(
                AbortMultipartUploadRequest.builder()
                        .bucket(bucketProvider.getBucketName())
                        .uploadId(uploadId)
                        .key(key)
                        .build()
        );
    }

    public CompleteMultipartUploadResponse completeMultipartUpload(CompleteMultipartRequest request){

        return client.completeMultipartUpload(
                CompleteMultipartUploadRequest
                        .builder()
                        .bucket(bucketProvider.getBucketName())
                        .key(request.getFilename())
                        .uploadId(request.getUploadId())
                        .multipartUpload(
                                CompletedMultipartUpload
                                        .builder()
                                        .parts(
                                                Arrays
                                                        .stream(request.getParts())
                                                        .map(
                                                                part -> CompletedPart
                                                                        .builder()
                                                                        .partNumber(part.getPartNumber())
                                                                        .eTag(part.geteTag())
                                                                        .build()
                                                        )
                                                        .toArray(CompletedPart[]::new)
                                        )
                                        .build()
                        )
                        .build()
        );

    }

    public CompleteMultipartUploadResponse completeMultipartUpload(String uploadId, String key, CompletedPart[] parts){

        return client.completeMultipartUpload(
                CompleteMultipartUploadRequest
                        .builder()
                        .bucket(bucketProvider.getBucketName())
                        .key(key)
                        .uploadId(uploadId)
                        .multipartUpload(
                                CompletedMultipartUpload
                                        .builder()
                                        .parts(parts)
                                        .build()
                        )
                        .build()
        );

    }

}