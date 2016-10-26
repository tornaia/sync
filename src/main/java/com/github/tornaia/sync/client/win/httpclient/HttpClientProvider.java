package com.github.tornaia.sync.client.win.httpclient;

import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HttpClientProvider {

    @Value("${sync.backend.server.scheme}")
    private String backendServerScheme;

    @Value("${sync.backend.server.host}")
    private String backendServerHost;

    @Value("${sync.backend.server.port}")
    private int backendServerPort;

    private final org.apache.http.client.HttpClient httpClient = HttpClientBuilder.
            create()
            .disableAutomaticRetries()
            .setMaxConnPerRoute(20)
            .setMaxConnTotal(20)
            .addInterceptorFirst(new FailOnErrorResponseInterceptor())
            .build();

    public org.apache.http.client.HttpClient get() {
        return httpClient;
    }

    public String getServerUrl() {
        return backendServerScheme + "://" + backendServerHost + ":" + backendServerPort;
    }
}
