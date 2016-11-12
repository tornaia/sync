package com.github.tornaia.sync.server.controller;

import com.amazonaws.services.s3.AmazonS3;
import com.github.tornaia.sync.server.service.FileCommandService;
import com.github.tornaia.sync.server.service.FileQueryService;
import com.github.tornaia.sync.server.service.exception.FileAlreadyExistsException;
import com.github.tornaia.sync.server.service.exception.FileNotFoundException;
import com.github.tornaia.sync.shared.api.CreateFileRequest;
import com.github.tornaia.sync.shared.api.CreateFileRequestBuilder;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import com.github.tornaia.sync.shared.util.SerializerUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.io.InputStream;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ActiveProfiles({"fongo", "findify-s3"})
@WebMvcTest(FileController.class)
public class FileControllerTest {

    @MockBean
    private FileCommandService fileCommandService;

    @MockBean
    private FileQueryService fileQueryService;

    @MockBean
    private AmazonS3 s3Client;

    @Autowired
    private SerializerUtils serializerUtils;

    @Autowired
    private MockMvc mvc;

    @Test
    public void createFile() throws Exception {
        Mockito.doReturn(new FileMetaInfo("id", "userid", "path", 1024L, 1L, 2L))
                .when(fileCommandService).createFile(anyString(), anyString(), anyLong(), anyLong(), anyLong(), anyString(), any(InputStream.class));

        CreateFileRequest createFileRequest = new CreateFileRequestBuilder()
                .userid("userid")
                .size(1024L)
                .creationDateTime(1L)
                .modificationDateTime(2L)
                .create();

        mvc.perform(
                fileUpload("/api/files")
                        .file(new MockMultipartFile("fileAttributes", null, MediaType.APPLICATION_JSON_VALUE, serializerUtils.toJSON(createFileRequest).getBytes()))
                        .file(new MockMultipartFile("file", "4.txt", MediaType.APPLICATION_OCTET_STREAM_VALUE, "fileContent".getBytes()))
                        .param("clientid", "abc-clientid"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"OK\",\"fileMetaInfo\":{\"id\":\"id\",\"userid\":\"userid\",\"relativePath\":\"path\",\"size\":1024,\"creationDateTime\":1,\"modificationDateTime\":2},\"message\":null}"));
    }

    @Test
    public void createIfFileExists() throws Exception {
        doThrow(FileAlreadyExistsException.class)
                .when(fileCommandService).createFile(anyString(), anyString(), anyLong(), anyLong(), anyLong(), anyString(), any(InputStream.class));

        CreateFileRequest createFileRequest = new CreateFileRequestBuilder()
                .userid("userid")
                .size(1000L)
                .creationDateTime(1L)
                .modificationDateTime(2L)
                .create();

        mvc.perform(
                fileUpload("/api/files")
                        .file(new MockMultipartFile("fileAttributes", null, MediaType.APPLICATION_JSON_VALUE, serializerUtils.toJSON(createFileRequest).getBytes()))
                        .file(new MockMultipartFile("file", "4.txt", MediaType.APPLICATION_OCTET_STREAM_VALUE, "fileContent".getBytes()))
                        .param("clientid", "abc-clientid"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"ALREADY_EXIST\",\"fileMetaInfo\":null,\"message\":null}"));
    }

    @Test
    public void getIfFileDoesNotExist() throws Exception {
        doThrow(FileNotFoundException.class)
                .when(fileQueryService).getFileById("1000", "12");

        mvc.perform(get("/api/files/12?userid=1000"))
                .andExpect(status().isOk())
                .andExpect(content().bytes("".getBytes()));
    }
}
