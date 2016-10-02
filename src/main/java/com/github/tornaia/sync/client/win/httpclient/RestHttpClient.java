package com.github.tornaia.sync.client.win.httpclient;

import com.github.tornaia.sync.client.win.util.SerializerUtils;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
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
import java.net.URI;
import java.net.URISyntaxException;

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

    public FileMetaInfo onFileCreate(FileMetaInfo fileMetaInfo, File file) {
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
            System.out.println("File dizappeared? " + e.getMessage());
            return null;
        } catch (IOException e) {
            throw new RuntimeException("Push to server failed", e);
        }

        System.out.println("CREATE file: " + fileMetaInfo.relativePath + " (" + fileMetaInfo.length + ")");
        return SerializerUtils.toObject(response.getEntity(), FileMetaInfo.class);
    }

    public void onFileModify(FileMetaInfo fileMetaInfo, File file) {
        HttpEntity multipart = MultipartEntityBuilder
                .create()
                .addBinaryBody("file", file, ContentType.APPLICATION_OCTET_STREAM, fileMetaInfo.relativePath)
                .build();

        HttpPut httpPut = new HttpPut(SERVER_URL + FILE_PATH + "/" + fileMetaInfo.id + "?userid=" + USERID + "&creationDateTime=" + fileMetaInfo.creationDateTime + "&modificationDateTime=" + fileMetaInfo.modificationDateTime);
        httpPut.setEntity(multipart);

        try {
            httpClient.execute(httpPut);
        } catch (FileNotFoundException e) {
            System.out.println("File disappeared? " + e.getMessage());
            return;
        } catch (IOException e) {
            throw new RuntimeException("Push to server failed", e);
        }
        System.out.println("PUT file: " + fileMetaInfo.relativePath + " (" + fileMetaInfo.length + ")");
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
        System.out.println("DELETE file: " + fileMetaInfo.relativePath);
    }
}
