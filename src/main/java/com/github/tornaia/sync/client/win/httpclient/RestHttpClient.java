package com.github.tornaia.sync.client.win.httpclient;

import com.github.tornaia.sync.client.win.WinClientApp;
import com.github.tornaia.sync.client.win.util.SerializerUtils;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;

import static com.google.gson.internal.$Gson$Types.newParameterizedTypeWithOwner;

@Component
public class RestHttpClient {

    private static final Logger LOG = LoggerFactory.getLogger(WinClientApp.class);

    private static final String FILE_PATH = "/api/files";

    @Value("${server.scheme.http:http}")
    private String serverSchemeHttp;

    @Value("${server.host:127.0.0.1}")
    private String serverHost;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${frosch-sync.userid:7247234}")
    private String userid;

    private final HttpClient httpClient = HttpClientBuilder.
            create()
            .disableAutomaticRetries()
            .addInterceptorFirst(new FailOnErrorResponseInterceptor())
            .build();

    public RecentChangesResponse getAllAfter(long timestamp) {
        try {
            URI uri = new URIBuilder()
                    .setScheme(serverSchemeHttp)
                    .setHost(serverHost)
                    .setPort(serverPort)
                    .setPath(FILE_PATH)
                    .addParameter("userid", userid)
                    .addParameter("modificationDateTime", "" + timestamp)
                    .build();

            HttpGet httpGet = new HttpGet(uri);

            List<FileMetaInfo> response = httpClient.execute(httpGet, createListResponseHandler(FileMetaInfo.class));
            LOG.info("GET fileMetaInfos: " + response);
            return RecentChangesResponse.ok(response);
        } catch (URISyntaxException | IOException e) {
            return RecentChangesResponse.transferFailed(e.getMessage());
        }
    }

    public byte[] getFile(FileMetaInfo fileMetaInfo) {
        URI uri;
        try {
            uri = new URIBuilder()
                    .setScheme(serverSchemeHttp)
                    .setHost(serverHost)
                    .setPort(serverPort)
                    .setPath(FILE_PATH + "/" + fileMetaInfo.id)
                    .addParameter("userid", userid)
                    .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        HttpGet httpGet = new HttpGet(uri);

        byte[] response;
        try {
            response = IOUtils.toByteArray(httpClient.execute(httpGet).getEntity().getContent());
        } catch (IOException e) {
            throw new RuntimeException("Get from server failed", e);
        }

        LOG.info("GET file: " + fileMetaInfo);
        return response;
    }

    public FileCreateResponse onFileCreate(FileMetaInfo fileMetaInfo, File file) {
        HttpEntity multipart = MultipartEntityBuilder
                .create()
                .addBinaryBody("file", file, ContentType.APPLICATION_OCTET_STREAM, fileMetaInfo.relativePath)
                .build();

        HttpPost httpPost = new HttpPost(getServerUrl() + FILE_PATH + "?userid=" + userid + "&creationDateTime=" + fileMetaInfo.creationDateTime + "&modificationDateTime=" + fileMetaInfo.modificationDateTime);
        httpPost.setEntity(multipart);

        HttpResponse response;
        try {
            response = httpClient.execute(httpPost);
        } catch (FileNotFoundException e) {
            LOG.info("File disappeared meanwhile it was under upload(post)? " + e.getMessage());
            return FileCreateResponse.transferFailed(e.getMessage());
        } catch (IOException e) {
            return FileCreateResponse.transferFailed(e.getMessage());
        }

        if (Objects.equals(response.getStatusLine().getStatusCode(), HttpStatus.SC_CONFLICT)) {
            return FileCreateResponse.conflict(fileMetaInfo);
        }

        FileMetaInfo syncedFileMetaInfo = SerializerUtils.toObject(response.getEntity(), FileMetaInfo.class);
        LOG.info("CREATE file: " + syncedFileMetaInfo);
        return FileCreateResponse.ok(syncedFileMetaInfo);
    }


    public FileCreateResponse onFileModify(FileMetaInfo fileMetaInfo, File file) {
        HttpEntity multipart = MultipartEntityBuilder
                .create()
                .addBinaryBody("file", file, ContentType.APPLICATION_OCTET_STREAM, fileMetaInfo.relativePath)
                .build();

        HttpPut httpPut = new HttpPut(getServerUrl() + FILE_PATH + "/" + fileMetaInfo.id + "?userid=" + userid + "&creationDateTime=" + fileMetaInfo.creationDateTime + "&modificationDateTime=" + fileMetaInfo.modificationDateTime);
        httpPut.setEntity(multipart);

        HttpResponse response;
        try {
            response = httpClient.execute(httpPut);
        } catch (FileNotFoundException e) {
            LOG.info("File disappeared meanwhile it was under upload(put)? " + e.getMessage());
            return FileCreateResponse.transferFailed(e.getMessage());
        } catch (IOException e) {
            return FileCreateResponse.transferFailed(e.getMessage());
        }

        FileMetaInfo syncedFileMetaInfo = SerializerUtils.toObject(response.getEntity(), FileMetaInfo.class);
        LOG.info("PUT file: " + syncedFileMetaInfo);
        return FileCreateResponse.ok(syncedFileMetaInfo);
    }

    public void onFileDelete(FileMetaInfo fileMetaInfo) {
        URI uri;
        try {
            uri = new URIBuilder()
                    .setScheme(serverSchemeHttp)
                    .setHost(serverHost)
                    .setPort(serverPort)
                    .setPath(FILE_PATH + "/" + fileMetaInfo.id)
                    .addParameter("userid", userid)
                    .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        HttpDelete httpDelete = new HttpDelete(uri);
        httpDelete.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);

        try {
            httpClient.execute(httpDelete);
        } catch (IOException e) {
            throw new RuntimeException("Delete from server failed", e);
        }
        LOG.info("DELETE file: " + fileMetaInfo);
    }

    private static <T> ResponseHandler<List<T>> createListResponseHandler(Class<T> clazz) {
        return response -> {
            InputStreamReader inputStreamReader = new InputStreamReader(response.getEntity().getContent());
            String content = IOUtils.toString(inputStreamReader);
            Type type = newParameterizedTypeWithOwner(null, List.class, clazz);
            return new Gson().fromJson(content, type);
        };
    }

    private String getServerUrl() {
        return serverSchemeHttp + "://" + serverHost + ":" + serverPort;
    }
}
