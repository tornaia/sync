package com.github.tornaia.sync.client.win.httpclient;

import com.github.tornaia.sync.client.win.remote.NormalResponseStatusCodes;
import com.github.tornaia.sync.client.win.remote.SurvivableResponseStatusCodes;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.StatusLine;
import org.apache.http.protocol.HttpContext;

public class FailOnErrorResponseInterceptor implements HttpResponseInterceptor {

    @Override
    public void process(HttpResponse response, HttpContext context) {
        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();

        if (NormalResponseStatusCodes.isValid(statusCode)) {
            return;
        }
        if (SurvivableResponseStatusCodes.isSolvableByRepeat(statusCode)) {
            return;
        }

        throw new IllegalStateException("Unexpected response: " + response);
    }
}
