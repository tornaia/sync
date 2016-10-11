package com.github.tornaia.sync.server.controller;

import com.github.tornaia.sync.server.service.FileCommandService;
import com.github.tornaia.sync.server.service.FileQueryService;
import com.github.tornaia.sync.server.service.exception.FileAlreadyExistsException;
import com.github.tornaia.sync.server.service.exception.FileNotFoundException;
import com.github.tornaia.sync.shared.api.CreateFileRequest;
import com.github.tornaia.sync.shared.api.CreateFileRequestBuilder;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import com.google.gson.Gson;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(FileController.class)
public class FileControllerTest {

    @MockBean
    private FileCommandService fileCommandService;

    @MockBean
    private FileQueryService fileQueryService;

    @Autowired
    private MockMvc mvc;

    @Test
    public void createFile() throws Exception {
        Mockito.doReturn(new FileMetaInfo("id", "userid", "path", 1024L, 1L, 2L))
                .when(fileCommandService).createFile(anyString(), anyString(), anyLong(), anyLong(), anyString(), any(byte[].class));

        CreateFileRequest createFileRequest = new CreateFileRequestBuilder()
                .userid("userid")
                .creationDateTime(1L)
                .modificationDateTime(2L)
                .create();

        mvc.perform(
                fileUpload("/api/files")
                        .file(new MockMultipartFile("fileAttributes", null, MediaType.APPLICATION_JSON_VALUE, new Gson().toJson(createFileRequest).getBytes()))
                        .file(new MockMultipartFile("file", "4.txt", MediaType.APPLICATION_OCTET_STREAM_VALUE, "fileContent".getBytes()))
                        .param("clientid", "abc-clientid"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().json("{\"id\":\"id\",\"userid\":\"userid\",\"relativePath\":\"path\",\"length\":1024,\"creationDateTime\":1,\"modificationDateTime\":2}"));
    }

    @Test
    public void createIfFileExists() throws Exception {
        doThrow(FileAlreadyExistsException.class)
                .when(fileCommandService).createFile(anyString(), anyString(), anyLong(), anyLong(), anyString(), any(byte[].class));

        CreateFileRequest createFileRequest = new CreateFileRequestBuilder()
                .userid("userid")
                .creationDateTime(1L)
                .modificationDateTime(2L)
                .create();

        mvc.perform(
                fileUpload("/api/files")
                        .file(new MockMultipartFile("fileAttributes", null, MediaType.APPLICATION_JSON_VALUE, new Gson().toJson(createFileRequest).getBytes()))
                        .file(new MockMultipartFile("file", "4.txt", MediaType.APPLICATION_OCTET_STREAM_VALUE, "fileContent".getBytes()))
                        .param("clientid", "abc-clientid"))
                .andExpect(status().isConflict());
    }

    @Test
    public void getIfFileDoesNotExist() throws Exception {
        doThrow(FileNotFoundException.class)
                .when(fileQueryService).getFileById("12");

        mvc.perform(
                get("/api/files/12"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getIfMetaInfoDoesNotExist() throws Exception {
        doThrow(FileNotFoundException.class)
                .when(fileQueryService).getFileMetaInfoById("12");

        mvc.perform(
                get("/api/files/12/metaInfo"))
                .andExpect(status().isNotFound());
    }
}
