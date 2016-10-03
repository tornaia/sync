package com.github.tornaia.sync.client.win.httpclient;

import com.github.tornaia.sync.client.win.util.SerializerUtils;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
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

    private static final String SERVER_SCHEME = "http";
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 8080;
    private static final String SERVER_URL = SERVER_SCHEME + "://" + SERVER_HOST + ":" + SERVER_PORT;

    private static final String FILE_PATH = "/api/files";

    private static final String USERID = "7247234";

    private final HttpClient httpClient = HttpClientBuilder.
            create()
            .disableAutomaticRetries()
            .addInterceptorFirst(new FailOnErrorResponseInterceptor())
            .build();

    public SyncChangesResponse getAllAfter(long timestamp) {
        try {
            URI uri = new URIBuilder()
                    .setScheme(SERVER_SCHEME)
                    .setHost(SERVER_HOST)
                    .setPort(SERVER_PORT)
                    .setPath(FILE_PATH)
                    .addParameter("userid", USERID)
                    .addParameter("modificationDateTime", "" + timestamp)
                    .build();

            HttpGet httpGet = new HttpGet(uri);

            List<FileMetaInfo> response = httpClient.execute(httpGet, createListResponseHandler(FileMetaInfo.class));
            System.out.println("GET fileMetaInfos: " + response);
            return SyncChangesResponse.ok(response);
        } catch (URISyntaxException | IOException e) {
            return SyncChangesResponse.transferFailed(e.getMessage());
        }
    }

    public byte[] getFile(FileMetaInfo fileMetaInfo) {
        URI uri;
        try {
            uri = new URIBuilder()
                    .setScheme(SERVER_SCHEME)
                    .setHost(SERVER_HOST)
                    .setPort(SERVER_PORT)
                    .setPath(FILE_PATH + "/" + fileMetaInfo.id)
                    .addParameter("userid", USERID)
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

        System.out.println("GET file: " + response);
        return response;
    }

    public SyncResult onFileCreate(FileMetaInfo fileMetaInfo, File file) {
        HttpEntity multipart = MultipartEntityBuilder
                .create()
                .addBinaryBody("file", file, ContentType.APPLICATION_OCTET_STREAM, fileMetaInfo.relativePath)
                .build();

        HttpPost httpPost = new HttpPost(SERVER_URL + FILE_PATH + "?userid=" + USERID + "&creationDateTime=" + fileMetaInfo.creationDateTime + "&modificationDateTime=" + fileMetaInfo.modificationDateTime);
        httpPost.setEntity(multipart);

        HttpResponse response;
        try {
            response = httpClient.execute(httpPost);
        } catch (FileNotFoundException e) {
            System.out.println("File disappeared meanwhile it was under upload(post)? " + e.getMessage());
            return SyncResult.terminated(fileMetaInfo);
        } catch (IOException e) {
            throw new RuntimeException("Push to server failed", e);
        }

        if (Objects.equals(response.getStatusLine().getStatusCode(), 409)) {
            return SyncResult.conflict(fileMetaInfo);
        }

        FileMetaInfo syncedFileMetaInfo = SerializerUtils.toObject(response.getEntity(), FileMetaInfo.class);
        System.out.println("CREATE file: " + syncedFileMetaInfo);
        return SyncResult.ok(syncedFileMetaInfo);
    }

    public SyncResult onFileModify(FileMetaInfo fileMetaInfo, File file) {
        HttpEntity multipart = MultipartEntityBuilder
                .create()
                .addBinaryBody("file", file, ContentType.APPLICATION_OCTET_STREAM, fileMetaInfo.relativePath)
                .build();

        HttpPut httpPut = new HttpPut(SERVER_URL + FILE_PATH + "/" + fileMetaInfo.id + "?userid=" + USERID + "&creationDateTime=" + fileMetaInfo.creationDateTime + "&modificationDateTime=" + fileMetaInfo.modificationDateTime);
        httpPut.setEntity(multipart);

        HttpResponse response;
        try {
            response = httpClient.execute(httpPut);
        } catch (FileNotFoundException e) {
            System.out.println("File disappeared meanwhile it was under upload(put)? " + e.getMessage());
            return SyncResult.terminated(fileMetaInfo);
        } catch (IOException e) {
            throw new RuntimeException("Push to server failed", e);
        }

        FileMetaInfo syncedFileMetaInfo = SerializerUtils.toObject(response.getEntity(), FileMetaInfo.class);
        System.out.println("PUT file: " + syncedFileMetaInfo);
        return SyncResult.ok(syncedFileMetaInfo);
    }

    public void onFileDelete(FileMetaInfo fileMetaInfo) {
        URI uri;
        try {
            uri = new URIBuilder()
                    .setScheme(SERVER_SCHEME)
                    .setHost(SERVER_HOST)
                    .setPort(SERVER_PORT)
                    .setPath(FILE_PATH + "/" + fileMetaInfo.id)
                    .addParameter("userid", USERID)
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
        System.out.println("DELETE file: " + fileMetaInfo);
    }

    private static <T> ResponseHandler<List<T>> createListResponseHandler(Class<T> clazz) {
        return response -> {
            InputStreamReader inputStreamReader = new InputStreamReader(response.getEntity().getContent());
            String content = IOUtils.toString(inputStreamReader);
            Type type = newParameterizedTypeWithOwner(null, List.class, clazz);
            return new Gson().fromJson(content, type);
        };
    }
}
