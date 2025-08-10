package com.spreadsheet_parser.service.http.impl;

import com.spreadsheet_parser.service.http.HttpService;
import com.spreadsheet_parser.service.http.exception.RestException;
import com.spreadsheet_parser.service.http.model.HttpMethod;
import jakarta.annotation.PreDestroy;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.HttpEntities;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.util.Timeout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class ApacheHttpService implements HttpService {

    private static final Logger LOGGER = LogManager.getLogger(ApacheHttpService.class);

    private final CloseableHttpClient closeableHttpClient;

    public ApacheHttpService(){

        PoolingHttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder
                .create()
                .setMaxConnTotal(100)
                .setMaxConnPerRoute(100)
                .setConnPoolPolicy(PoolReusePolicy.LIFO)
                .setPoolConcurrencyPolicy(PoolConcurrencyPolicy.STRICT)
                .setDefaultConnectionConfig(
                        ConnectionConfig
                                .custom()
                                .setConnectTimeout(Timeout.ofSeconds(5))
                                .build()
                )
                .build();

        this.closeableHttpClient = HttpClients.custom().setConnectionManager(cm).build();

    }

    @Override
    public String doPost(String hostAddress, String resourcePath, String payload, Map<String, String> headers, Map<String, String> parameters, int requestTimeout) {
        return doRequestWithPayload(
                HttpMethod.POST,
                hostAddress,
                resourcePath,
                HttpEntities.create(
                        payload,
                        ContentType.create(ContentType.APPLICATION_JSON.getMimeType(), StandardCharsets.UTF_8)
                ),
                headers,
                parameters,
                requestTimeout
        );
    }

    private String doRequestWithPayload(
            HttpMethod method
            , String hostAddress
            , String resourcePath
            , HttpEntity httpEntity
            , Map<String, String> headers
            , Map<String, String> parameters
            , int requestTimeout
    ){

        try {

            URIBuilder uriBuilder = new URIBuilder(hostAddress);
            uriBuilder.setPath(resourcePath);

            HttpUriRequestBase request;
            switch (method){

                case POST:
                    request = new HttpPost(uriBuilder.build());
                    break;

                case PUT:
                    request = new HttpPut(uriBuilder.build());
                    break;

                case PATCH:
                    request = new HttpPatch(uriBuilder.build());
                    break;

                default:
                    throw new RuntimeException(method + " HTTP method can't have payload");

            }

            if(parameters != null){
                for(Map.Entry<String, String> entry : parameters.entrySet()) uriBuilder.addParameter(entry.getKey(), entry.getValue());
            }

            if(headers != null){
                for(Map.Entry<String, String> entry : headers.entrySet()) request.setHeader(entry.getKey(), entry.getValue());
            }

            request.setConfig(
                    RequestConfig
                            .custom()
                            .setResponseTimeout(Timeout.ofMilliseconds(requestTimeout))
                            .setConnectionRequestTimeout(Timeout.ofMinutes(requestTimeout))
                            .build()
            );
            if(httpEntity != null) request.setEntity(httpEntity);

            return closeableHttpClient.execute(request, stringResponseHandler);

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

    }

    private static final HttpClientResponseHandler<String> stringResponseHandler = response -> {

        int status = response.getCode();
        if (status >= 200 && status < 300) return response.getEntity() != null ? EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8) : null;
        else{

            Map<String, String> headers = new HashMap<>();
            for(Header header : response.getHeaders()) headers.put(header.getName(), header.getValue());

            throw new RestException(status, (response.getEntity() != null ? EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8) : String.valueOf(status)), headers);

        }

    };

    @PreDestroy
    public void shutDown(){
        try {
            LOGGER.info("Closing HTTP Closeable Client");
            if(closeableHttpClient != null) closeableHttpClient.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
