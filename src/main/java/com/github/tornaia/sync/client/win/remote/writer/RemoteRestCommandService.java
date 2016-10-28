package com.github.tornaia.sync.client.win.remote.writer;

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
        HttpEntity entity = null;
        try {
            HttpResponse response = httpClientProvider.get().execute(httpPost);
            entity = response.getEntity();
            if (Objects.equals(response.getStatusLine().getStatusCode(), HttpStatus.SC_CONFLICT)) {
                return FileCreateResponse.conflict(fileMetaInfo);
            }

            if (SurvivableResponseStatusCodes.isSolvableByRepeat(response.getStatusLine().getStatusCode())) {
                return FileCreateResponse.transferFailed(fileMetaInfo, response.getStatusLine().getReasonPhrase());
            }

            remoteFileMetaInfo = serializerUtils.toObject(entity.getContent(), FileMetaInfo.class);
        } catch (FileNotFoundException e) {
            LOG.debug("File disappeared meanwhile it was under upload(post)? " + e.getMessage());
            return FileCreateResponse.transferFailed(fileMetaInfo, e.getMessage());
        } catch (IOException e) {
            return FileCreateResponse.transferFailed(fileMetaInfo, e.getMessage());
        } finally {
            httpClientProvider.consumeEntity(entity);
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

        HttpEntity entity = null;
        try {
            HttpResponse response = httpClientProvider.get().execute(httpPut);
            entity = response.getEntity();
            int statusCode = response.getStatusLine().getStatusCode();
            if (Objects.equals(statusCode, HttpStatus.SC_CONFLICT)) {
                return FileModifyResponse.conflict(fileMetaInfo);
            }

            if (Objects.equals(statusCode, HttpStatus.SC_NOT_FOUND)) {
                return FileModifyResponse.notFound(fileMetaInfo);
            }

            FileMetaInfo remoteFileMetaInfo = serializerUtils.toObject(entity, FileMetaInfo.class);

            LOG.debug("Modified file: " + fileMetaInfo);
            return FileModifyResponse.ok(remoteFileMetaInfo);

        } catch (FileNotFoundException e) {
            LOG.debug("File disappeared meanwhile it was under upload(post)? " + e.getMessage());
            return FileModifyResponse.transferFailed(fileMetaInfo, e.getMessage());
        } catch (IOException e) {
            return FileModifyResponse.transferFailed(fileMetaInfo, e.getMessage());
        } finally {
            httpClientProvider.consumeEntity(entity);
        }
    }

    public FileDeleteResponse onFileDelete(FileMetaInfo fileMetaInfo) {
        DeleteFileRequest deleteFileRequest = new DeleteFileRequestBuilder()
                .id(fileMetaInfo.id)
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
                .addPart(generateJsonFormBodyPart("fileAttributes", serializerUtils.toJSON(deleteFileRequest)))
                .build();

        HttpPost httpPost = new HttpPost(httpClientProvider.getServerUrl() + backendFileApiPath + "/delete/" + fileMetaInfo.id + "?clientid=" + clientidService.clientid);
        httpPost.setEntity(multipart);

        HttpEntity entity = null;
        try {
            HttpResponse response = httpClientProvider.get().execute(httpPost);
            entity = response.getEntity();

            if (Objects.equals(response.getStatusLine().getStatusCode(), HttpStatus.SC_NOT_FOUND)) {
                return FileDeleteResponse.notFound(fileMetaInfo, "Servers does not know about this file");
            }

            if (Objects.equals(response.getStatusLine().getStatusCode(), HttpStatus.SC_CONFLICT)) {
                return FileDeleteResponse.conflict(fileMetaInfo, "Servers does not allow to delete this file");
            }

            LOG.debug("Deleted file: " + fileMetaInfo);
            return FileDeleteResponse.ok(fileMetaInfo);
        } catch (IOException e) {
            return FileDeleteResponse.transferFailed(fileMetaInfo, e.getMessage());
        } finally {
            httpClientProvider.consumeEntity(entity);
        }
    }

    // http://stackoverflow.com/questions/35675679/apache-http-4-5-stringbody-constructor-not-exporting-content-type-in-request
    private FormBodyPart generateJsonFormBodyPart(String name, String myJsonStuff) {
        StringBody json = new StringBody(myJsonStuff, ContentType.APPLICATION_JSON); //<--THE GOGGLES, THEY DO NOTHING!!

        StringBuilder buffer = new StringBuilder();
        buffer.append("form-data" + "; name=\"" + name + "\"");
        buffer.append("\r\n");
        buffer.append("Content-Type: application/json;charset=UTF-8"); //<--tack this on to the

        String kludgeForDispositionAndContentType = buffer.toString();

        FormBodyPartBuilder partBuilder = FormBodyPartBuilder.create(name, json);
        partBuilder.setField(MIME.CONTENT_DISPOSITION, kludgeForDispositionAndContentType);

        return partBuilder.build();
    }
}
