package com.spreadsheet_parser.service.synchronizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Primary
public class WorkerThreadFactory implements ThreadFactory {

    private static final Logger LOGGER = LogManager.getLogger(WorkerThreadFactory.class);

    private final String poolName = "spreadsheet-parser";
    final AtomicInteger threadCount = new AtomicInteger(0);

    @Override
    public Thread newThread(Runnable r) {

        Thread thread = Executors.defaultThreadFactory().newThread(r);
        thread.setName(poolName + "-thread-" + String.format("%d", threadCount.getAndIncrement()));

        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread t, Throwable e) {
                LOGGER.error("UnCaught Exception Handler", e);
            }

        };
        thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);

        return thread;

    }

}
