package com.github.tornaia.sync.client.win.remote.reader;

import com.github.tornaia.sync.client.win.httpclient.HttpClientProvider;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import com.github.tornaia.sync.shared.api.GetFileResponseStatus;
import com.github.tornaia.sync.shared.exception.SerializerException;
import org.apache.commons.io.IOUtils;
import org.apache.http.ConnectionClosedException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Objects;

import static com.github.tornaia.sync.shared.api.GetFileResponseStatus.FILE_STATUS_HEADER_FIELD_NAME;

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

        HttpEntity entity = null;
        try {
            HttpResponse response = httpClientProvider.get().execute(httpGet);
            entity = response.getEntity();
            // TODO here we have now some memory limitation: redesign to use inputStream instead of byte[]. OOM error might occur
            byte[] content = IOUtils.toByteArray(entity.getContent());

            Header fileStatusHeader = response.getFirstHeader(FILE_STATUS_HEADER_FIELD_NAME);
            String fileStatusValue = fileStatusHeader.getValue();
            GetFileResponseStatus fileStatus = GetFileResponseStatus.valueOf(fileStatusValue);

            boolean ok = Objects.equals(fileStatus, GetFileResponseStatus.OK);
            if (ok) {
                return FileGetResponse.ok(fileMetaInfo, content);
            }

            boolean notFound = Objects.equals(fileStatus, GetFileResponseStatus.NOT_FOUND);
            if (notFound) {
                return FileGetResponse.notFound(fileMetaInfo);
            }

            boolean transferFailed = Objects.equals(fileStatus, GetFileResponseStatus.TRANSFER_FAILED);
            if (transferFailed) {
                return FileGetResponse.transferFailed(fileMetaInfo, "Server side");
            }

            boolean unknownProblem = Objects.equals(fileStatus, GetFileResponseStatus.UNKNOWN_PROBLEM);
            if (unknownProblem) {
                throw new IllegalStateException("Unhandled state");
            }

            LOG.debug("GET file: " + fileMetaInfo);
            return FileGetResponse.ok(fileMetaInfo, content);
        } catch (SerializerException | SocketException | ConnectionClosedException | SocketTimeoutException e) {
            return FileGetResponse.transferFailed(fileMetaInfo, "Client side: " + e.getMessage());
        } catch (IOException e) {
            throw new IllegalStateException("Unhandled state", e);
        } finally {
            httpClientProvider.consumeEntity(entity);
        }
    }
}
