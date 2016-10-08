package com.github.tornaia.sync.client.win.httpclient;

import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HttpClientProvider {

    @Value("${server.scheme.http:http}")
    private String serverSchemeHttp;

    @Value("${server.host:127.0.0.1}")
    private String serverHost;

    @Value("${server.port:8080}")
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
