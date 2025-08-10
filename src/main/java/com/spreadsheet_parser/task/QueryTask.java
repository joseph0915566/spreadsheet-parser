package com.spreadsheet_parser.task;

import com.spreadsheet_parser.service.aws.s3.S3AsyncService;
import com.spreadsheet_parser.service.c3po.C3poService;
import com.spreadsheet_parser.service.observability.datadog.C3poMetric;
import com.spreadsheet_parser.service.observability.datadog.S3Metric;
import io.micrometer.core.instrument.Timer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class QueryTask {

    private static final Logger LOGGER = LogManager.getLogger(QueryTask.class);

    private final S3AsyncService s3AsyncService;
    private final C3poService c3poService;
    private final C3poMetric c3poMetric;
    private final S3Metric s3Metric;
    private final int s3SelectRetryDelay;
    private final int s3SelectRetryCount;
    private final int c3poRetryDelay;
    private final int c3poRetryCount;
    private final int batchSize;

    @Inject
    public QueryTask(
            C3poService c3poService,
            S3AsyncService s3AsyncService,
            C3poMetric c3poMetric,
            S3Metric s3Metric,
            @Value("${aws.s3.select.retry.delay}") int s3SelectRetryDelay,
            @Value("${aws.s3.select.retry.count}") int s3SelectRetryCount,
            @Value("${3po.retry.count}") int c3poRetryCount,
            @Value("${3po.retry.delay}") int c3poRetryDelay,
            @Value("${3po.batchsize}") int batchSize
    ){
        this.s3AsyncService = s3AsyncService;
        this.c3poService = c3poService;
        this.c3poMetric = c3poMetric;
        this.s3Metric = s3Metric;
        this.s3SelectRetryDelay = s3SelectRetryDelay;
        this.s3SelectRetryCount = s3SelectRetryCount;
        this.c3poRetryDelay = c3poRetryDelay;
        this.c3poRetryCount = c3poRetryCount;
        this.batchSize = batchSize;
    }

    public String processBatch(MainTask.Joiner joiner, String uploadId, String key, int offset) throws Exception {
        joiner
                .join(
                        query3Po(
                                uploadId,
                                key,
                                offset,
                                queryS3Select(
                                        uploadId,
                                        key,
                                        offset,
                                        batchSize
                                )
                        )
                );
        return "";
    }

    private String queryS3Select(String uploadId, String key, int offset, int batchSize){

        Timer.Sample sample = s3Metric.getSample();
        try {

            String result = s3AsyncService.getRowBatchFromCsv(key, offset, batchSize);
            sample.stop(s3Metric.getTimer(uploadId, key, offset, true));

            return result;

        } catch (Exception t){
            sample.stop(s3Metric.getTimer(uploadId, key, offset, false));
            LOGGER.error("Failed for " + uploadId + "_" + offset, t);
            throw t;
        }

    }

    private String query3Po(String uploadId, String key, int offset, String csv){

        Timer.Sample sample = c3poMetric.getSample();
        try {

            String result = c3poService.getTopMatch("name,country\n" + csv);
            sample.stop(c3poMetric.getTimer(uploadId, key, offset, true));

            return result;

        } catch (Exception t){
            sample.stop(c3poMetric.getTimer(uploadId, key, offset, false));
            LOGGER.error("Failed for " + uploadId + "_" + offset, t);
            throw t;
        }

    }

}
