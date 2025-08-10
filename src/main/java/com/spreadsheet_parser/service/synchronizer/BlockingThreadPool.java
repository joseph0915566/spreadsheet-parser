package com.spreadsheet_parser.service.synchronizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class BlockingThreadPool {

    private static final Logger LOGGER = LogManager.getLogger(BlockingThreadPool.class);

    private final ThreadPoolExecutor threadPool;
    private final int workerCount;
    private final Lock lock = new ReentrantLock();
    private final Condition isNotBusy = lock.newCondition();
    private int count = 0;

    public BlockingThreadPool(ThreadFactory threadFactory, int maxWorkerCount){
        this.workerCount = maxWorkerCount;
        this.threadPool = new ThreadPoolExecutor(
                1,
                maxWorkerCount,
                5,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    public<T> Future<T> submit(Callable<T> job){

        Future<T> future = null;
        lock.lock();
        try {

            while(count >= workerCount){
                LOGGER.info("Waiting. Cur thread count {} counter {} max allowed {}", threadPool.getPoolSize(), count, workerCount);
                isNotBusy.await();
                LOGGER.info("Wake up signal received");
            }

            count++;
            LOGGER.info("Submitting work. Current thread count {} counter {}", threadPool.getPoolSize(), count);
            future = threadPool.submit(() -> {

                try {
                    return job.call();
                } finally {

                    lock.lock();
                    try {
                        count--;
                        isNotBusy.signalAll();
                    } finally {
                        LOGGER.info("Finished work. Current thread count {} counter {}", threadPool.getPoolSize(), count);
                        lock.unlock();
                    }

                }

            });

            LOGGER.info("Work submitted. Current thread count {} counter {}", threadPool.getPoolSize(), count);

        } catch (InterruptedException e) {
            LOGGER.error("Thread interrupted", e);
            Thread.currentThread().interrupt();
        } finally {
            LOGGER.info("Current thread count {} counter {}", threadPool.getPoolSize(), count);
            lock.unlock();
        }

        return future;

    }

}
