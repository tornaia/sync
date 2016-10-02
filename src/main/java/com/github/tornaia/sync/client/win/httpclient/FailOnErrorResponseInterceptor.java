package com.github.tornaia.sync.client.win.httpclient;

import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HttpContext;

import java.util.Objects;

public class FailOnErrorResponseInterceptor implements HttpResponseInterceptor {

    @Override
    public void process(HttpResponse response, HttpContext context) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (!Objects.equals(statusCode, HttpStatus.SC_OK)) {
            throw new IllegalStateException("Non-OK response: " + response);
        }
    }
}
