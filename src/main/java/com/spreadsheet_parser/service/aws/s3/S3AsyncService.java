package com.spreadsheet_parser.service.aws.s3;

import com.spreadsheet_parser.service.aws.CredentialProvider;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class S3AsyncService {

    private final S3AsyncClient asyncClient;
    private final BucketProvider bucketProvider;

    @Inject
    public S3AsyncService(
            BucketProvider bucketProvider,
            CredentialProvider credentialProvider
    ){
        this.bucketProvider = bucketProvider;
        this.asyncClient = S3AsyncClient
                .builder()
                .credentialsProvider(credentialProvider.get())
                .region(bucketProvider.getRegion())
                .build();
    }

    public String getRowBatchFromCsv(String key, int offset, int limit){

        SelectObjectContentRequest request = SelectObjectContentRequest
                .builder()
                .bucket(bucketProvider.getBucketName())
                .key(key)
                .expression(String.format("SELECT s._2, s._1 FROM s3object s WHERE CAST(s._3 as INT) > %d LIMIT %d", offset, limit))
                .expressionType(ExpressionType.SQL)
                .inputSerialization(
                        InputSerialization
                                .builder()
                                .csv(
                                        CSVInput
                                                .builder()
                                                .fileHeaderInfo(FileHeaderInfo.NONE)
                                                .fieldDelimiter(",")
                                                .recordDelimiter("\n")
                                                .build()
                                )
                                .compressionType(CompressionType.NONE)
                                .build()
                )
                .outputSerialization(
                        OutputSerialization
                                .builder()
                                .csv(
                                        CSVOutput
                                                .builder()
                                                .fieldDelimiter(",")
                                                .recordDelimiter("\n")
                                                .build()
                                )
                                .build()
                )
                .build();

        try {

            ResponseHandler responseHandler = new ResponseHandler();
            asyncClient.selectObjectContent(request, responseHandler).get();

            return ((RecordsEvent) responseHandler.events.stream()
                    .filter(e -> e.sdkEventType() == SelectObjectContentEventStream.EventType.RECORDS)
                    .findFirst()
                    .orElse(null)).payload().asUtf8String();

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

    }

    private static class ResponseHandler implements SelectObjectContentResponseHandler {

        private SelectObjectContentResponse response;
        private List<SelectObjectContentEventStream> events = new ArrayList<>();
        private Throwable throwable;

        @Override
        public void responseReceived(SelectObjectContentResponse response) {
            this.response = response;
        }

        @Override
        public void onEventStream(SdkPublisher<SelectObjectContentEventStream> publisher) {
            publisher.subscribe(this.events::add);
        }

        @Override
        public void exceptionOccurred(Throwable throwable) {
            this.throwable = throwable;
        }

        @Override
        public void complete() {}

    }

}
