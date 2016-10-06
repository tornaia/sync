package com.github.tornaia.sync.server.controller;

import com.github.tornaia.sync.server.service.FileCommandService;
import com.github.tornaia.sync.server.service.FileQueryService;
import com.github.tornaia.sync.server.service.exception.FileAlreadyExistsException;
import com.github.tornaia.sync.server.service.exception.FileNotFoundException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

        mvc.perform(
                fileUpload("/api/files")
                        .file(file)
                        .param("userid", "userid")
                        .param("creationDateTime", "1")
                        .param("modificationDateTime", "2"))
                .andExpect(status().isOk());
    }

    @Test
    public void createIfFileExists() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "file.png", "image/png", "test".getBytes());

        doThrow(FileAlreadyExistsException.class)
                .when(fileCommandService).createFile(anyString(), anyLong(), anyLong(), anyString(), any(byte[].class));

        mvc.perform(
                fileUpload("/api/files")
                        .file(file)
                        .param("userid", "userid")
                        .param("creationDateTime", "1")
                        .param("modificationDateTime", "2"))
                .andExpect(status().isConflict());
    }

    @Test
    public void getIfFileDoesNotExist() throws Exception {
        doThrow(FileNotFoundException.class)
                .when(fileQueryService).getFileById("12");

        mvc.perform(
                get("/api/files/12")
                        .param("userid", "userid"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getIfMetaInfoDoesNotExist() throws Exception {
        doThrow(FileNotFoundException.class)
                .when(fileQueryService).getFileMetaInfoById("12");

        mvc.perform(
                get("/api/files/12/metaInfo")
                        .param("userid", "userid"))
                .andExpect(status().isNotFound());
    }
}
