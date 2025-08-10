package com.spreadsheet_parser.task;

import com.spreadsheet_parser.service.aws.s3.S3Service;
import com.spreadsheet_parser.service.mail.ResultEmailService;
import com.spreadsheet_parser.service.synchronizer.BlockingThreadPool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service
public class CompanyMatchTaskRunner {

    private final S3Service s3Service;
    private final BlockingThreadPool queryThreadPool;
    private final BlockingThreadPool uploaderThreadPool;
    private final ResultEmailService resultEmailService;
    private final QueryTask queryTask;
    private final int batchSize;

    @Inject
    public CompanyMatchTaskRunner(
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

    public void runTask(String key, int totalRows, String email){
        new MainTask(
                s3Service,
                queryThreadPool,
                uploaderThreadPool,
                resultEmailService,
                queryTask,
                batchSize
        )
                .process(key, totalRows, email);
    }

}
