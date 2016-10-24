package com.github.tornaia.sync.client.win.remote.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tornaia.sync.client.win.ClientidService;
import com.github.tornaia.sync.client.win.httpclient.HttpClientProvider;
import com.github.tornaia.sync.client.win.remote.SurvivableResponseStatusCodes;
import com.github.tornaia.sync.shared.api.*;
import com.github.tornaia.sync.shared.util.SerializerUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
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

    @Value("${sync.backend.file.api.path}")
    private String backendFileApiPath;

    @Value("${client.sync.userid}")
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

        HttpPost httpPost = new HttpPost(httpClientProvider.getServerUrl() + backendFileApiPath + "?clientid=" + clientidService.clientid);
        httpPost.setEntity(multipart);

        FileMetaInfo remoteFileMetaInfo;
        try {
            HttpResponse response = httpClientProvider.get().execute(httpPost);
            HttpEntity entity = response.getEntity();
            if (Objects.equals(response.getStatusLine().getStatusCode(), HttpStatus.SC_CONFLICT)) {
                EntityUtils.consume(entity);
                return FileCreateResponse.conflict(fileMetaInfo);
            }

            if (SurvivableResponseStatusCodes.isSolvableByRepeat(response.getStatusLine().getStatusCode())) {
                return FileCreateResponse.transferFailed(fileMetaInfo, response.getStatusLine().getReasonPhrase());
            }

            remoteFileMetaInfo = new ObjectMapper().readValue(entity.getContent(), FileMetaInfo.class);
        } catch (FileNotFoundException e) {
            LOG.debug("File disappeared meanwhile it was under upload(post)? " + e.getMessage());
            return FileCreateResponse.transferFailed(fileMetaInfo, e.getMessage());
        } catch (IOException e) {
            return FileCreateResponse.transferFailed(fileMetaInfo, e.getMessage());
        }

        LOG.debug("Created file: " + fileMetaInfo);
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

        HttpPut httpPut = new HttpPut(httpClientProvider.getServerUrl() + backendFileApiPath + "/" + fileMetaInfo.id + "?clientid=" + clientidService.clientid);
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
            LOG.debug("File disappeared meanwhile it was under upload(post)? " + e.getMessage());
            return FileModifyResponse.transferFailed(fileMetaInfo, e.getMessage());
        } catch (IOException e) {
            return FileModifyResponse.transferFailed(fileMetaInfo, e.getMessage());
        }

        FileMetaInfo remoteFileMetaInfo = serializerUtils.toObject(entity, FileMetaInfo.class);

        LOG.debug("Modified file: " + fileMetaInfo);
        return FileModifyResponse.ok(remoteFileMetaInfo);
    }

    public FileDeleteResponse onFileDelete(FileMetaInfo fileMetaInfo) {
        DeleteFileRequest deleteFileRequest = new DeleteFileRequestBuilder()
                .id(fileMetaInfo.id)
                .size(fileMetaInfo.size)
                .creationDateTime(fileMetaInfo.creationDateTime)
                .modificationDateTime(fileMetaInfo.modificationDateTime)
                .create();

        HttpEntity multipart = MultipartEntityBuilder
                .create()
                .setCharset(StandardCharsets.UTF_8)
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .setContentType(ContentType.MULTIPART_FORM_DATA)
                .addPart(generateJsonFormBodyPart("fileAttributes", serializerUtils.toJSON(deleteFileRequest)))
                .build();

        HttpPost httpPost = new HttpPost(httpClientProvider.getServerUrl() + backendFileApiPath + "/delete/" + fileMetaInfo.id + "?clientid=" + clientidService.clientid);
        httpPost.setEntity(multipart);

        try {
            HttpResponse response = httpClientProvider.get().execute(httpPost);
            HttpEntity entity = response.getEntity();
            EntityUtils.consume(entity);

            if (Objects.equals(response.getStatusLine().getStatusCode(), HttpStatus.SC_NOT_FOUND)) {
                EntityUtils.consume(entity);
                return FileDeleteResponse.notFound(fileMetaInfo, "Servers does not know about this file");
            }
        } catch (IOException e) {
            return FileDeleteResponse.transferFailed(fileMetaInfo, e.getMessage());
        }

        LOG.debug("Deleted file: " + fileMetaInfo);
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
