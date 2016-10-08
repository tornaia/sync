package com.github.tornaia.sync.server.controller;

import com.github.tornaia.sync.server.service.FileCommandService;
import com.github.tornaia.sync.server.service.FileQueryService;
import com.github.tornaia.sync.server.service.exception.FileNotFoundException;
import com.github.tornaia.sync.shared.api.GetFileMetaInfoRequest;
import com.github.tornaia.sync.shared.api.GetFileRequest;
import com.google.gson.Gson;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
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

    // TODO https://github.com/springfox/springfox/issues/588
//    @Test
//    public void createFile() throws Exception {
//        MockMultipartFile file = new MockMultipartFile("file", "file.png", "image/png", "test".getBytes());
//
//        doNothing()
//                .when(fileCommandService).createFile(anyString(), anyLong(), anyLong(), anyString(), any(byte[].class));
//
//        MockMultipartFile jsonFile = new MockMultipartFile("json", "", "application/json", "{\"json\": \"someValue\"}".getBytes());
//
//        mvc.perform(fileUpload("/api/files")
//                .file(file)
//                .merge(MockMvcRequestBuilders.request(HttpMethod.POST, "/api/files")
//                .contentType(MediaType.APPLICATION_JSON)
//                        .
//                .content())
//                .param("fileAttributes", new Gson().toJson(new CreateFileRequest("userid", 1L, 2L))))
//                // .content(new Gson().toJson(new CreateFileRequest("userid", 1L, 2L))))
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    public void createIfFileExists() throws Exception {
//        MockMultipartFile file = new MockMultipartFile("file", "file.png", "image/png", "test".getBytes());
//
//        doThrow(FileAlreadyExistsException.class)
//                .when(fileCommandService).createFile(anyString(), anyLong(), anyLong(), anyString(), any(byte[].class));
//
//        mvc.perform(
//                fileUpload("/api/files")
//                        .file(file).contentType(MediaType.APPLICATION_JSON_UTF8)
//                        .content(new Gson().toJson(new CreateFileRequest("userid", 1L, 2L))))
//                .andExpect(status().isConflict());
//    }

    @Test
    public void getIfFileDoesNotExist() throws Exception {
        doThrow(FileNotFoundException.class)
                .when(fileQueryService).getFileById("12");

        mvc.perform(
                get("/api/files/12").content(new Gson().toJson(new GetFileRequest("userid")))
                        .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getIfMetaInfoDoesNotExist() throws Exception {
        doThrow(FileNotFoundException.class)
                .when(fileQueryService).getFileMetaInfoById("12");

        mvc.perform(
                get("/api/files/12/metaInfo")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(new Gson().toJson(new GetFileMetaInfoRequest("userid"))))
                .andExpect(status().isNotFound());
    }
}
