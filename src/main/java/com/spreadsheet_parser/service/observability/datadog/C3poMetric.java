package com.spreadsheet_parser.service.observability.datadog;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service
public class C3poMetric {

    private final Timer.Builder timer;
    private final MeterRegistry registry;

    @Inject
    public C3poMetric(MeterRegistry registry){
        this.registry = registry;
        this.timer = Timer.builder("spreadsheet-parser.3po.duration");
    }

    public Timer.Sample getSample(){
        return Timer.start(registry);
    }

    public Timer getTimer(String uploadId, String key, int offset, boolean isSuccess){
        String[] keyParts = key.split("/");
        return timer
                .tag("ldap", keyParts[0])
                .tag("uploadId", uploadId)
                .tag("batch", uploadId + "_" + offset)
                .tag("success", String.valueOf(isSuccess))
                .register(registry);
    }

}
