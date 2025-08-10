package com.spreadsheet_parser.service.synchronizer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.concurrent.ThreadFactory;

@Service
@Primary
public class CompanyMatchingThreadPool extends BlockingThreadPool{

    @Inject
    public CompanyMatchingThreadPool(ThreadFactory threadFactory, @Value("${company-matching.workercount}") int workerCount) {
        super(threadFactory, workerCount);
    }

}
