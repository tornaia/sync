package com.github.tornaia.sync.client.win.remote.writer;

import com.github.tornaia.sync.client.win.ClientidService;
import com.github.tornaia.sync.client.win.httpclient.HttpClientProvider;
import com.github.tornaia.sync.shared.api.*;
import com.github.tornaia.sync.shared.util.SerializerUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
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
import java.util.Optional;

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

    public CreateFileResponse onFileCreate(FileMetaInfo fileMetaInfo, File file) {
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


        HttpEntity entity = null;
        try {
            HttpResponse response = httpClientProvider.get().execute(httpPost);
            entity = response.getEntity();
            Optional<CreateFileResponse> optionalCreateFileResponse = serializerUtils.toObject(entity, CreateFileResponse.class);
            if (!optionalCreateFileResponse.isPresent()) {
                return CreateFileResponse.transferFailed("Malformed response: " + response);
            }
            CreateFileResponse createFileResponse = optionalCreateFileResponse.get();
            LOG.debug("Created file: " + createFileResponse.fileMetaInfo);
            return createFileResponse;
        } catch (FileNotFoundException e) {
            LOG.debug("File disappeared meanwhile it was under upload(post)? " + e.getMessage());
            return CreateFileResponse.transferFailed(e.getMessage());
        } catch (IOException e) {
            return CreateFileResponse.transferFailed(e.getMessage());
        } finally {
            httpClientProvider.consumeEntity(entity);
        }
    }

    public ModifyFileResponse onFileModify(FileMetaInfo fileMetaInfo, File file) {
        ModifyFileRequest modifyFileRequest = new ModifyFileRequestBuilder()
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
                        serializerUtils.toJSON(modifyFileRequest)))
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
            Optional<ModifyFileResponse> optionalModifyFileResponse = serializerUtils.toObject(entity, ModifyFileResponse.class);
            if (!optionalModifyFileResponse.isPresent()) {
                return ModifyFileResponse.transferFailed("Malformed response: " + response);
            }
            ModifyFileResponse modifyFileResponse = optionalModifyFileResponse.get();
            LOG.debug("Modified file: " + modifyFileResponse.fileMetaInfo);
            return modifyFileResponse;
        } catch (FileNotFoundException e) {
            LOG.debug("File disappeared meanwhile it was under upload(post)? " + e.getMessage());
            return ModifyFileResponse.transferFailed(e.getMessage());
        } catch (IOException e) {
            return ModifyFileResponse.transferFailed(e.getMessage());
        } finally {
            httpClientProvider.consumeEntity(entity);
        }
    }

    public DeleteFileResponse onFileDelete(FileMetaInfo fileMetaInfo) {
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
            Optional<DeleteFileResponse> optionalDeleteFileResponse = serializerUtils.toObject(entity, DeleteFileResponse.class);
            if (!optionalDeleteFileResponse.isPresent()) {
                return DeleteFileResponse.transferFailed("Malformed response: " + response);
            }
            DeleteFileResponse deleteFileResponse = optionalDeleteFileResponse.get();
            LOG.debug("Deleted file: " + fileMetaInfo);
            return deleteFileResponse;
        } catch (IOException e) {
            return DeleteFileResponse.transferFailed(e.getMessage());
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
