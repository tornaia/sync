package com.github.tornaia.sync.client.win.remote.reader;

import com.github.tornaia.sync.client.win.httpclient.HttpClientProvider;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.SocketException;
import java.util.Objects;

@Component
public class RemoteRestQueryService {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteRestQueryService.class);

    @Value("${sync.backend.file.api.path}")
    private String backendFileApiPath;

    @Value("${client.sync.userid}")
    private String userid;

    @Autowired
    private HttpClientProvider httpClientProvider;

    public FileGetResponse getFile(FileMetaInfo fileMetaInfo) {
        HttpGet httpGet = new HttpGet(httpClientProvider.getServerUrl() + backendFileApiPath + "/" + fileMetaInfo.id + "?userid=" + userid);

        try {
            HttpResponse response = httpClientProvider.get().execute(httpGet);
            HttpEntity entity = response.getEntity();
            // TODO here we have now some memory limitation: redesign to use inputStream instead of byte[]. OOM error might occur
            byte[] content = IOUtils.toByteArray(entity.getContent());

            int statusCode = response.getStatusLine().getStatusCode();
            boolean ok = Objects.equals(statusCode, HttpStatus.SC_OK);
            if (ok) {
                LOG.debug("GET file: " + fileMetaInfo);
                return FileGetResponse.ok(fileMetaInfo, content);
            }

            boolean notFound = Objects.equals(statusCode, HttpStatus.SC_NOT_FOUND);
            if (notFound) {
                return FileGetResponse.notFound(fileMetaInfo);
            }

            return FileGetResponse.transferFailed(fileMetaInfo, "Transfer failed: " + statusCode);
        } catch (SocketException e) {
            return FileGetResponse.transferFailed(fileMetaInfo, "Transfer failed: " + HttpStatus.SC_REQUEST_TIMEOUT);
        } catch (IOException e) {
            throw new RuntimeException("Get from server failed", e);
        }
    }
}
