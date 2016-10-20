package com.github.tornaia.sync.client.win.httpclient;

import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HttpClientProvider {

    @Value("${sync.backend.server.scheme}")
    private String serverSchemeHttp;

    @Value("${sync.backend.server.host}")
    private String serverHost;

    @Value("${sync.backend.server.port}")
    private int serverPort;

    private final org.apache.http.client.HttpClient httpClient = HttpClientBuilder.
            create()
            .disableAutomaticRetries()
            .addInterceptorFirst(new FailOnErrorResponseInterceptor())
            .build();

    public org.apache.http.client.HttpClient get() {
        return httpClient;
    }

    public String getServerUrl() {
        return serverSchemeHttp + "://" + serverHost + ":" + serverPort;
    }
}
