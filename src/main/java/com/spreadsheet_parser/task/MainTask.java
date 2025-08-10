package com.spreadsheet_parser.task;

import com.spreadsheet_parser.service.aws.s3.S3Service;
import com.spreadsheet_parser.service.mail.ResultEmailService;
import com.spreadsheet_parser.service.synchronizer.BlockingThreadPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.services.s3.model.CompletedPart;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MainTask {

    private static final Logger LOGGER = LogManager.getLogger(MainTask.class);

    private final S3Service s3Service;
    private final BlockingThreadPool queryThreadPool;
    private final BlockingThreadPool uploaderThreadPool;
    private final ResultEmailService resultEmailService;
    private final QueryTask queryTask;
    private final int batchSize;

    public MainTask(
            S3Service s3Service,
            BlockingThreadPool queryThreadPool,
            @Qualifier("s3UploadThreadPool") BlockingThreadPool uploaderThreadPool,
            ResultEmailService resultEmailService,
            QueryTask queryTask,
            @Value("${3po.batchsize}") int batchSize
    ){
        this.s3Service = s3Service;
        this.queryThreadPool = queryThreadPool;
        this.resultEmailService = resultEmailService;
        this.uploaderThreadPool = uploaderThreadPool;
        this.queryTask = queryTask;
        this.batchSize = batchSize;
    }

    public void process(String key, int totalRows, String recipientEmail) {

        String resultKey = key.substring(0, key.lastIndexOf(".")) + "_company_matching_result.csv";
        String uploadId = s3Service.generateUploadId(resultKey);

        List<Future<String>> futures = new ArrayList<>();
        Joiner joiner = new Joiner(uploadId, resultKey);
        for(int offset = 0 ; offset < totalRows ; offset += batchSize) {
            LOGGER.info("Processing {} / {}", offset, totalRows);
            final int start = offset;
            futures.add(queryThreadPool.submit(() -> queryTask.processBatch(joiner, uploadId, key, start)));
        }

        try {
            for (Future<String> future : futures) future.get();
            s3Service.completeMultipartUpload(uploadId, resultKey, joiner.getParts());
        } catch (InterruptedException e) {
            LOGGER.error("Thread interrupted", e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            LOGGER.error("Execution Exception", e);
            throw new RuntimeException(e);
        }

        resultEmailService.sendResultEmail(recipientEmail, resultKey);

    }

    class Joiner {

        private static final int S3_MINIMUM_CHARACTER_COUNT = 10 * 1024 * 1024;

        private final Lock lock = new ReentrantLock();
        private final List<Future<String>> futures = new ArrayList<>();
        private final String uploadId;
        private final String key;

        private int partNumber = 0;
        private StringBuilder stringBuilder = new StringBuilder(
                "name," +
                        "country," +
                        "normalized_name," +
                        "advertiser_id," +
                        "advertiser_name," +
                        "prospect_id," +
                        "prospect_name," +
                        "fcc_id," +
                        "non_company\n"
        );

        private Joiner(String uploadId, String key){
            this.uploadId = uploadId;
            this.key = key;
        }

        public void join(String payload){
            lock.lock();
            try {
                stringBuilder.append(payload);
                if(stringBuilder.length() >= S3_MINIMUM_CHARACTER_COUNT){
                    partNumber++;
                    uploadPart();
                    stringBuilder = new StringBuilder();
                }
            } finally {
                lock.unlock();
            }
        }

        public CompletedPart[] getParts() throws ExecutionException, InterruptedException {

            lock.lock();
            try {

                if(stringBuilder.length() > 0){
                    partNumber++;
                    uploadPart();
                }

                CompletedPart[] parts = new CompletedPart[futures.size()];
                int index = 1;
                for(Future<String> future : futures) parts[index - 1] = CompletedPart
                        .builder()
                        .eTag(future.get())
                        .partNumber(index++)
                        .build();

                return parts;

            } finally {
                lock.unlock();
            }

        }

        private void uploadPart(){
            futures.add(uploaderThreadPool.submit(() -> s3Service.uploadPart(uploadId, key, partNumber, stringBuilder.toString())));
        }

    }

}
