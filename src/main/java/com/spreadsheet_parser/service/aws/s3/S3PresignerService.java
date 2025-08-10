package com.spreadsheet_parser.service.aws.s3;

import com.spreadsheet_parser.service.aws.CredentialProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

import javax.inject.Inject;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class S3PresignerService {

    private final S3Presigner s3Presigner;
    private final BucketProvider bucketProvider;
    private final long uploadUrlDurationHour;
    private final int downloadUrlDurationDay;

    @Inject
    public S3PresignerService(
            BucketProvider bucketProvider,
            CredentialProvider credentialProvider,
            @Value("${aws.s3.upload.url-expiry-hour}") int uploadUrlDurationHour,
            @Value("${aws.s3.download.url-expiry-day}") int downloadUrlDurationDay
    ) {
        this.bucketProvider = bucketProvider;
        this.s3Presigner = S3Presigner
                .builder()
                .credentialsProvider(credentialProvider.get())
                .region(bucketProvider.getRegion())
                .build();
        this.downloadUrlDurationDay = downloadUrlDurationDay;
        this.uploadUrlDurationHour = uploadUrlDurationHour;
    }

    public Map<Integer, String> generateUploadUrls(String uploadId, int partCount, String key){

        Map<Integer, String> urls = new HashMap<>();
        for(int i = 1 ; i <= partCount ; i++){
            urls.put(
                    i,
                    s3Presigner
                            .presignUploadPart(
                                    UploadPartPresignRequest
                                            .builder()
                                            .uploadPartRequest(
                                                    UploadPartRequest
                                                            .builder()
                                                            .bucket(bucketProvider.getBucketName())
                                                            .key(key)
                                                            .uploadId(uploadId)
                                                            .partNumber(i)
                                                            .build()
                                            )
                                            .signatureDuration(Duration.ofHours(uploadUrlDurationHour))
                                            .build()
                            )
                            .url()
                            .toString()
            );
        }

        return urls;

    }

    public String generateDownloadUrl(String key){
        return s3Presigner
                .presignGetObject(
                        GetObjectPresignRequest
                                .builder()
                                .signatureDuration(Duration.ofDays(downloadUrlDurationDay))
                                .getObjectRequest(
                                        GetObjectRequest
                                                .builder()
                                                .bucket(bucketProvider.getBucketName())
                                                .key(key)
                                                .build()
                                )
                                .build()
                )
                .url()
                .toString();
    }

}
