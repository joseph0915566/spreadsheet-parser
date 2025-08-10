package com.spreadsheet_parser.service.c3po;

import com.spreadsheet_parser.service.http.HttpService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service
public class C3poService {

    private final HttpService httpService;
    private final int requestTimeout;
    private final String serviceAddress;

    @Inject
    public C3poService(
            HttpService httpService,
            @Value("${3po.hostname}") String serviceAddress,
            @Value("${3po.timeout.request}") int requestTimeout
    ){
        this.httpService = httpService;
        this.serviceAddress = serviceAddress;
        this.requestTimeout = requestTimeout;
    }

    public String getTopMatch(String csv){
        return httpService.doPost(serviceAddress, "api/company/top-match", csv, null, null, requestTimeout);
    }

}
