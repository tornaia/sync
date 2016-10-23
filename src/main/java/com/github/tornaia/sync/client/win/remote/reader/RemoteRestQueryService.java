package com.github.tornaia.sync.client.win.remote.reader;

import com.github.tornaia.sync.client.win.httpclient.HttpClientProvider;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RemoteRestQueryService {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteRestQueryService.class);

    private static final String FILE_PATH = "/api/files";

    @Autowired
    private HttpClientProvider httpClientProvider;

    @Value("${client.sync.userid}")
    private String userid;

    public byte[] getFile(FileMetaInfo fileMetaInfo) {
        HttpGet httpGet = new HttpGet(httpClientProvider.getServerUrl() + FILE_PATH + "/" + fileMetaInfo.id + "?userid=" + userid);

        byte[] response;
        try {
            HttpEntity entity = httpClientProvider.get().execute(httpGet).getEntity();
            // FIXME here sometimes we get 406 (0 content length is causing this)
            // FIXME here we have a memory limitation: redesign to use inputstream instead of byte[]. 5GB file failed here with OOM
            response = IOUtils.toByteArray(entity.getContent());
            EntityUtils.consume(entity);
        } catch (IOException e) {
            throw new RuntimeException("Get from server failed", e);
        }

        LOG.debug("GET file: " + fileMetaInfo);
        return response;
    }
}
