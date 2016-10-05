package com.github.tornaia.sync.server.controller;

import com.github.tornaia.sync.server.data.document.File;
import com.github.tornaia.sync.server.data.repository.FileRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(FileController.class)
public class FileControllerTest {

  @MockBean
  private FileRepository fileRepository;

  @Autowired
  private MockMvc mvc;

  @Test
  public void createFile() throws Exception {
    MockMultipartFile file = new MockMultipartFile("file", "file.png", "image/png", "test".getBytes());

    given(fileRepository.findByPath(file.getOriginalFilename()))
      .willReturn(null);
    given(fileRepository.insert(any(File.class)))
      .willReturn(new File(file.getOriginalFilename(), file.getBytes(), "userid", 1L, 2L));

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

    given(fileRepository.findByPath(file.getOriginalFilename()))
      .willReturn(new File());

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
    given(fileRepository.findOne("12"))
      .willReturn(null);

    mvc.perform(
      get("/api/files/12")
        .param("userid", "userid"))
      .andExpect(status().isNotFound());
  }

  @Test
  public void getIfMetaInfoDoesNotExist() throws Exception {
    given(fileRepository.findOne("12"))
      .willReturn(null);

    mvc.perform(
      get("/api/files/12/metaInfo")
        .param("userid", "userid"))
      .andExpect(status().isNotFound());
  }

}
