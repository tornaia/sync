package com.github.tornaia.sync.client.win.remote.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tornaia.sync.client.win.ClientidService;
import com.github.tornaia.sync.client.win.httpclient.HttpClientProvider;
import com.github.tornaia.sync.client.win.util.SerializerUtils;
import com.github.tornaia.sync.shared.api.*;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.*;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Component
public class RemoteRestCommandService {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteRestCommandService.class);

    private static final String FILE_PATH = "/api/files";

    @Value("${frosch-sync.userid:7247234}")
    private String userid;

    @Autowired
    private HttpClientProvider httpClientProvider;

    @Autowired
    private ClientidService clientidService;

    @Autowired
    private SerializerUtils serializerUtils;

    public FileCreateResponse onFileCreate(FileMetaInfo fileMetaInfo, File file) {
        CreateFileRequest createFileRequest = new CreateFileRequestBuilder()
                .userid(userid)
                .creationDateTime(fileMetaInfo.creationDateTime)
                .modificationDateTime(fileMetaInfo.modificationDateTime)
                .create();

        HttpEntity multipart = MultipartEntityBuilder
                .create()
                .setCharset(StandardCharsets.UTF_8)
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .setContentType(ContentType.MULTIPART_FORM_DATA)
                .addPart(generateJsonFormBodyPart("fileAttributes",
                        serializerUtils.toJSON(createFileRequest)))
                .addPart(FormBodyPartBuilder.create()
                        .setName("file")
                        .setBody(new FileBody(file, ContentType.APPLICATION_OCTET_STREAM, fileMetaInfo.relativePath))
                        .build())
                .build();

        // TODO query params are not needed anymore
        HttpPost httpPost = new HttpPost(httpClientProvider.getServerUrl() + FILE_PATH + "?userid=" + userid + "&creationDateTime=" + fileMetaInfo.creationDateTime + "&modificationDateTime=" + fileMetaInfo.modificationDateTime + "&clientid=" + clientidService.clientid);
        httpPost.setEntity(multipart);

        HttpResponse response;
        try {
            response = httpClientProvider.get().execute(httpPost);
        } catch (FileNotFoundException e) {
            LOG.info("File disappeared meanwhile it was under upload(post)? " + e.getMessage());
            return FileCreateResponse.transferFailed(fileMetaInfo, e.getMessage());
        } catch (IOException e) {
            return FileCreateResponse.transferFailed(fileMetaInfo, e.getMessage());
        }

        if (Objects.equals(response.getStatusLine().getStatusCode(), HttpStatus.SC_CONFLICT)) {
            return FileCreateResponse.conflict(fileMetaInfo);
        }

        FileMetaInfo remoteFileMetaInfo;
        try {
            remoteFileMetaInfo = new ObjectMapper().readValue(response.getEntity().getContent(), FileMetaInfo.class);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected response", e);
        }

        return FileCreateResponse.ok(remoteFileMetaInfo);
    }

    public FileModifyResponse onFileModify(FileMetaInfo fileMetaInfo, File file) {
        UpdateFileRequest updateFileRequest = new UpdateFileRequestBuilder()
                .userid(userid)
                .creationDateTime(fileMetaInfo.creationDateTime)
                .modificationDateTime(fileMetaInfo.modificationDateTime)
                .create();

        HttpEntity multipart = MultipartEntityBuilder
                .create()
                .setCharset(StandardCharsets.UTF_8)
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .setContentType(ContentType.MULTIPART_FORM_DATA)
                .addPart(generateJsonFormBodyPart("fileAttributes",
                        serializerUtils.toJSON(updateFileRequest)))
                .addPart(FormBodyPartBuilder.create()
                        .setName("file")
                        .setBody(new FileBody(file, ContentType.APPLICATION_OCTET_STREAM, fileMetaInfo.relativePath))
                        .build())
                .build();

        HttpPut httpPut = new HttpPut(httpClientProvider.getServerUrl() + FILE_PATH + "/" + fileMetaInfo.id + "?clientid=" + clientidService.clientid);
        httpPut.setEntity(multipart);

        HttpResponse response;
        try {
            response = httpClientProvider.get().execute(httpPut);
        } catch (FileNotFoundException e) {
            LOG.info("File disappeared meanwhile it was under upload(post)? " + e.getMessage());
            return FileModifyResponse.transferFailed(fileMetaInfo, e.getMessage());
        } catch (IOException e) {
            return FileModifyResponse.transferFailed(fileMetaInfo, e.getMessage());
        }

        if (Objects.equals(response.getStatusLine().getStatusCode(), HttpStatus.SC_CONFLICT)) {
            return FileModifyResponse.conflict(fileMetaInfo);
        }

        if (Objects.equals(response.getStatusLine().getStatusCode(), HttpStatus.SC_NOT_FOUND)) {
            return FileModifyResponse.notFound(fileMetaInfo);
        }

        FileMetaInfo remoteFileMetaInfo;
        try {
            remoteFileMetaInfo = new ObjectMapper().readValue(response.getEntity().getContent(), FileMetaInfo.class);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected response", e);
        }

        return FileModifyResponse.ok(remoteFileMetaInfo);
    }

    public void onFileDelete(FileMetaInfo fileMetaInfo) {
        HttpDelete httpDelete = new HttpDelete(httpClientProvider.getServerUrl() + FILE_PATH + "/" + fileMetaInfo.id + "?userid=" + userid + "&clientid=" + clientidService.clientid);
        httpDelete.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);

        try {
            httpClientProvider.get().execute(httpDelete);
        } catch (IOException e) {
            throw new RuntimeException("Delete from server failed", e);
        }
        LOG.info("DELETE file: " + fileMetaInfo);
    }

    // http://stackoverflow.com/questions/35675679/apache-http-4-5-stringbody-constructor-not-exporting-content-type-in-request
    private FormBodyPart generateJsonFormBodyPart(String name, String myJsonStuff) {
        StringBody json = new StringBody(myJsonStuff, ContentType.APPLICATION_JSON); //<--THE GOGGLES, THEY DO NOTHING!!

        StringBuilder buffer = new StringBuilder();
        buffer.append("form-data" + "; name=\"" + name + "\"");
        buffer.append("\r\n");
        buffer.append("Content-Type: application/json"); //<--tack this on to the

        String kludgeForDispositionAndContentType = buffer.toString();

        FormBodyPartBuilder partBuilder = FormBodyPartBuilder.create(name, json);
        partBuilder.setField(MIME.CONTENT_DISPOSITION, kludgeForDispositionAndContentType);

        return partBuilder.build();
    }
}
