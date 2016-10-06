package com.github.tornaia.sync.server.controller;

import com.github.tornaia.sync.server.data.document.File;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import com.github.tornaia.sync.shared.api.matchers.FileMetaInfoMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import utils.AbstractSyncServerIntTest;

import java.util.List;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
public class FileControllerIntTest extends AbstractSyncServerIntTest {

    @Autowired
    private FileController fileController;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void getMetaInfo() throws Exception {
        MockMultipartFile file = new MockMultipartFile("test", "test.png", "image/png", "TEST".getBytes());
        fileController.postFile("userid", 1L, 2L, file);

        List<FileMetaInfo> modifiedFiles = fileController.getModifiedFiles("userid", -1);
        FileMetaInfo result = fileController.getMetaInfo(modifiedFiles.get(0).id, "userid");

        FileMetaInfoMatcher expected = new FileMetaInfoMatcher()
                .relativePath("test.png")
                .creationDateTime(1L)
                .modificationDateTime(2L)
                .length(4L);

        assertThat(result, expected);

    }

    @Test
    public void putFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("test", "test.png", "image/png", "TEST".getBytes());
        fileController.postFile("userid", 2L, 3L, file);

        List<FileMetaInfo> modifiedFiles = fileController.getModifiedFiles("userid", -1);
        FileMetaInfo fileMetaInfo = fileController.getMetaInfo(modifiedFiles.get(0).id, "userid");

        MockMultipartFile updatedFile = new MockMultipartFile("test", "test.png", "image/png", "TEST2".getBytes());
        FileMetaInfo result = fileController.putFile(fileMetaInfo.id, "userid", 3L, 4L, updatedFile);

        FileMetaInfoMatcher expected = new FileMetaInfoMatcher()
                .relativePath("test.png")
                .creationDateTime(3L)
                .modificationDateTime(4L)
                .length(5L);

        assertThat(result, expected);
    }

    @Test
    public void deleteFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("test", "test.png", "image/png", "TEST".getBytes());
        fileController.postFile("userid", 1L, 2L, file);

        List<FileMetaInfo> modifiedFiles = fileController.getModifiedFiles("userid", -1);
        FileMetaInfo createdFile = fileController.getMetaInfo(modifiedFiles.get(0).id, "userid");

        fileController.deleteFile(createdFile.id);

        //TODO nxjohny: We can wrap it in the corresponding driver and hide mongotemplate op. The interface throws exception in case of null file, so i have to use the template to validate the delete whether was successful or not.
        File result = mongoTemplate.findById(createdFile.id, File.class);
        assertNull(result);
    }
}
