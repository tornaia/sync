package com.github.tornaia.sync.client.win.remote.reader;

import com.github.tornaia.sync.client.win.httpclient.HttpClientProvider;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
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

    public byte[] getFile(FileMetaInfo fileMetaInfo) {
        HttpGet httpGet = new HttpGet(httpClientProvider.getServerUrl() + backendFileApiPath + "/" + fileMetaInfo.id + "?userid=" + userid);

        byte[] content;
        try {
            HttpResponse response = httpClientProvider.get().execute(httpGet);
            HttpEntity entity = response.getEntity();
            content = IOUtils.toByteArray(entity.getContent());
            boolean ok = Objects.equals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK);

            if (!ok) {
                EntityUtils.consume(entity);
                // FIXME introduce FileGetResponse (like FileDeleteResponse,FileCreateResponse,FileModifyResponse)...
                throw new NotImplementedException("When servers is not available or something goes wrong (here) then the logic might break");
            }
            // TODO here we have a memory limitation: redesign to use inputStream instead of byte[]. 5GB file failed here with OOM
            EntityUtils.consume(entity);
        } catch (IOException e) {
            throw new RuntimeException("Get from server failed", e);
        }

        LOG.debug("GET file: " + fileMetaInfo);
        return content;
    }
}
