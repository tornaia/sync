package com.github.tornaia.sync.client.win.httpclient;

import com.github.tornaia.sync.client.win.remote.NormalResponseStatusCodes;
import com.github.tornaia.sync.client.win.remote.SurvivableResponseStatusCodes;
import com.github.tornaia.sync.shared.api.GetFileResponseStatus;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.tornaia.sync.shared.api.GetFileResponseStatus.FILE_STATUS_HEADER_FIELD_NAME;

public class FailOnErrorResponseInterceptor implements HttpResponseInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(FailOnErrorResponseInterceptor.class);

    @Override
    public void process(HttpResponse response, HttpContext context) {
        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();

        if (NormalResponseStatusCodes.isValid(statusCode)) {
            return;
        }
        if (SurvivableResponseStatusCodes.isSolvableByRepeat(statusCode)) {
            LOG.warn("Network problem: " + response);
            return;
        }

        boolean isNotAcceptable = statusCode == HttpStatus.SC_NOT_ACCEPTABLE;
        boolean hasFileStatusHeader = response.getFirstHeader(FILE_STATUS_HEADER_FIELD_NAME) != null;
        if (isNotAcceptable && hasFileStatusHeader) {
            LOG.warn("NOT_ACCEPTABLE with File-Status OK means the InputStream from S3 to Client is closed unexpectedly somewhere: " + response);
            response.removeHeaders(FILE_STATUS_HEADER_FIELD_NAME);
            response.addHeader(FILE_STATUS_HEADER_FIELD_NAME, GetFileResponseStatus.TRANSFER_FAILED.name());
            return;
        }

        throw new IllegalStateException("Unexpected response: " + response);
    }
}
