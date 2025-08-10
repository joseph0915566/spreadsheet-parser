package com.spreadsheet_parser.service.observability.datadog;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service
public class S3Metric {

    private final Timer.Builder s3SelectTimer;
    private final MeterRegistry registry;

    @Inject
    public S3Metric(MeterRegistry registry){
        this.registry = registry;
        this.s3SelectTimer = Timer.builder("SelfServiceAppDaemon.aws.s3.select.duration");
    }

    public Timer.Sample getSample(){
        return Timer.start(registry);
    }

    public Timer getTimer(String uploadId, String key, int offset, boolean isSuccess){
        String[] keyParts = key.split("/");
        return s3SelectTimer
                .tag("ldap", keyParts[0])
                .tag("uploadId", uploadId)
                .tag("batch", uploadId + "_" + offset)
                .tag("success", String.valueOf(isSuccess))
                .register(registry);
    }

}
