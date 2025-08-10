package com.spreadsheet_parser.service.synchronizer;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadFactory;

@Service
@Qualifier("s3UploadThreadPool")
public class S3UploadThreadPool extends BlockingThreadPool{

    public S3UploadThreadPool(ThreadFactory threadFactory, @Value("${aws.s3.workercount}") int maxWorkerCount) {
        super(threadFactory, maxWorkerCount);
    }

}
