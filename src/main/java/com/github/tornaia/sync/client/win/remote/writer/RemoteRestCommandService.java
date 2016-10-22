package com.github.tornaia.sync.client.win.remote.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tornaia.sync.client.win.ClientidService;
import com.github.tornaia.sync.client.win.httpclient.HttpClientProvider;
import com.github.tornaia.sync.shared.api.*;
import com.github.tornaia.sync.shared.util.SerializerUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.*;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Component
public class RemoteRestCommandService {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteRestCommandService.class);

    private static final String FILE_PATH = "/api/files";

    @Value("${frosch-sync.userid}")
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
                .relativePath(fileMetaInfo.relativePath)
                .size(fileMetaInfo.size)
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
                .addPart(file.isDirectory() ? null : FormBodyPartBuilder.create()
                        .setName("file")
                        .setBody(new FileBody(file, ContentType.APPLICATION_OCTET_STREAM, fileMetaInfo.relativePath))
                        .build())
                .build();

        HttpPost httpPost = new HttpPost(httpClientProvider.getServerUrl() + FILE_PATH + "?clientid=" + clientidService.clientid);
        httpPost.setEntity(multipart);

        FileMetaInfo remoteFileMetaInfo;
        try {
            HttpResponse response = httpClientProvider.get().execute(httpPost);
            HttpEntity entity = response.getEntity();
            if (Objects.equals(response.getStatusLine().getStatusCode(), HttpStatus.SC_CONFLICT)) {
                EntityUtils.consume(entity);
                return FileCreateResponse.conflict(fileMetaInfo);
            }

            // FIXME this error might occur everywhere... refactor this and the writer class
            if (Objects.equals(response.getStatusLine().getStatusCode(), HttpStatus.SC_BAD_GATEWAY)) {
                return FileCreateResponse.transferFailed(fileMetaInfo, "Bad gateway");
            }

            remoteFileMetaInfo = new ObjectMapper().readValue(entity.getContent(), FileMetaInfo.class);
        } catch (FileNotFoundException e) {
            LOG.info("File disappeared meanwhile it was under upload(post)? " + e.getMessage());
            return FileCreateResponse.transferFailed(fileMetaInfo, e.getMessage());
        } catch (IOException e) {
            return FileCreateResponse.transferFailed(fileMetaInfo, e.getMessage());
        }

        return FileCreateResponse.ok(remoteFileMetaInfo);
    }

    public FileModifyResponse onFileModify(FileMetaInfo fileMetaInfo, File file) {
        UpdateFileRequest updateFileRequest = new UpdateFileRequestBuilder()
                .userid(userid)
                .size(fileMetaInfo.size)
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
                .addPart(file.isDirectory() ? null : FormBodyPartBuilder.create()
                        .setName("file")
                        .setBody(new FileBody(file, ContentType.APPLICATION_OCTET_STREAM, fileMetaInfo.relativePath))
                        .build())
                .build();

        HttpPut httpPut = new HttpPut(httpClientProvider.getServerUrl() + FILE_PATH + "/" + fileMetaInfo.id + "?clientid=" + clientidService.clientid);
        httpPut.setEntity(multipart);

        HttpEntity entity;
        try {
            HttpResponse response = httpClientProvider.get().execute(httpPut);
            entity = response.getEntity();
            if (Objects.equals(response.getStatusLine().getStatusCode(), HttpStatus.SC_CONFLICT)) {
                EntityUtils.consume(entity);
                return FileModifyResponse.conflict(fileMetaInfo);
            }

            if (Objects.equals(response.getStatusLine().getStatusCode(), HttpStatus.SC_NOT_FOUND)) {
                EntityUtils.consume(entity);
                return FileModifyResponse.notFound(fileMetaInfo);
            }
        } catch (FileNotFoundException e) {
            LOG.info("File disappeared meanwhile it was under upload(post)? " + e.getMessage());
            return FileModifyResponse.transferFailed(fileMetaInfo, e.getMessage());
        } catch (IOException e) {
            return FileModifyResponse.transferFailed(fileMetaInfo, e.getMessage());
        }

        FileMetaInfo remoteFileMetaInfo;
        try {
            remoteFileMetaInfo = new ObjectMapper().readValue(entity.getContent(), FileMetaInfo.class);
        } catch (IOException e) {
            LOG.error("Ok response with a malformed response body?", e);
            return FileModifyResponse.transferFailed(fileMetaInfo, e.getMessage());
        }

        return FileModifyResponse.ok(remoteFileMetaInfo);
    }

    public FileDeleteResponse onFileDelete(FileMetaInfo fileMetaInfo) {
        HttpDelete httpDelete = new HttpDelete(httpClientProvider.getServerUrl() + FILE_PATH + "/" + fileMetaInfo.id + "?clientid=" + clientidService.clientid);
        try {
            HttpResponse response = httpClientProvider.get().execute(httpDelete);
            HttpEntity entity = response.getEntity();
            EntityUtils.consume(entity);
        } catch (IOException e) {
            return FileDeleteResponse.transferFailed(fileMetaInfo, e.getMessage());
        }

        LOG.info("DELETE file: " + fileMetaInfo);
        return FileDeleteResponse.ok(fileMetaInfo);
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
