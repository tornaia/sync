package com.github.tornaia.sync.server.controller;

import com.github.tornaia.sync.server.service.FileCommandService;
import com.github.tornaia.sync.server.service.FileQueryService;
import com.github.tornaia.sync.server.service.exception.FileAlreadyExistsException;
import com.github.tornaia.sync.server.service.exception.FileNotFoundException;
import com.github.tornaia.sync.shared.api.CreateFileRequest;
import com.github.tornaia.sync.shared.api.GetFileMetaInfoRequest;
import com.github.tornaia.sync.shared.api.GetFileRequest;
import com.github.tornaia.sync.shared.api.UpdateFileRequest;
import com.google.gson.Gson;
import com.sun.org.apache.xpath.internal.SourceTree;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doNothing;
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
        MockMultipartFile file = new MockMultipartFile("file", "file.png", "image/png", "test".getBytes());

        doNothing()
                .when(fileCommandService).createFile(anyString(), anyLong(), anyLong(), anyString(), any(byte[].class));
        Gson gson = new Gson();
        mvc.perform(
                fileUpload("/api/files")
                        .file(file).contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(gson.toJson(new CreateFileRequest("userid", 1L, 2L))))
                .andExpect(status().isOk()).andDo(System.out::println);
    }

    @Test
    public void createIfFileExists() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "file.png", "image/png", "test".getBytes());

        doThrow(FileAlreadyExistsException.class)
                .when(fileCommandService).createFile(anyString(), anyLong(), anyLong(), anyString(), any(byte[].class));
        Gson gson = new Gson();
        mvc.perform(
                fileUpload("/api/files")
                        .file(file).contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(gson.toJson(new CreateFileRequest("userid", 1L, 2L))))
                .andExpect(status().isConflict());
    }

    @Test
    public void getIfFileDoesNotExist() throws Exception {
        doThrow(FileNotFoundException.class)
                .when(fileQueryService).getFileById("12");
        Gson gson = new Gson();
        mvc.perform(
                get("/api/files/12").content(gson.toJson(new GetFileRequest("userid")))
                        .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getIfMetaInfoDoesNotExist() throws Exception {
        doThrow(FileNotFoundException.class)
                .when(fileQueryService).getFileMetaInfoById("12");
        Gson gson = new Gson();
        mvc.perform(
                get("/api/files/12/metaInfo")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(gson.toJson(new GetFileMetaInfoRequest("userid"))))
                .andExpect(status().isNotFound());
    }
}
