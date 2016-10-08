package com.github.tornaia.sync.server.controller;

import com.github.tornaia.sync.server.data.document.File;
import com.github.tornaia.sync.shared.api.*;
import com.github.tornaia.sync.shared.api.matchers.FileMetaInfoMatcher;
import org.hamcrest.Matchers;
import org.junit.Test;
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

    @Test
    public void getMetaInfo() throws Exception {
        MockMultipartFile file = new MockMultipartFile("test", "test.png", "image/png", "TEST".getBytes());
        fileController.postFile(new CreateFileRequest("userid", 1L, 2L), file);

        List<FileMetaInfo> modifiedFiles = fileController.getModifiedFiles(new GetModifiedFilesRequest("userid", -1L));
        FileMetaInfo result = fileController.getMetaInfo(modifiedFiles.get(0).id, new GetFileMetaInfoRequest("userid"));

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
        fileController.postFile(new CreateFileRequest("userid", 2L, 3L), file);

        List<FileMetaInfo> modifiedFiles = fileController.getModifiedFiles(new GetModifiedFilesRequest("userid", -1L));
        FileMetaInfo fileMetaInfo = fileController.getMetaInfo(modifiedFiles.get(0).id, new GetFileMetaInfoRequest("userid"));

        MockMultipartFile updatedFile = new MockMultipartFile("test", "test.png", "image/png", "TEST2".getBytes());

        FileMetaInfo result = fileController.putFile(fileMetaInfo.id, new UpdateFileRequest("userid", 3L, 4L), updatedFile);

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
        fileController.postFile(new CreateFileRequest("userid", 1L, 2L), file);

        List<FileMetaInfo> modifiedFiles = fileController.getModifiedFiles(new GetModifiedFilesRequest("userid", -1L));
        FileMetaInfo createdFile = fileController.getMetaInfo(modifiedFiles.get(0).id, new GetFileMetaInfoRequest("userid"));

        fileController.deleteFile(createdFile.id);

        //TODO nxjohny: We can wrap it in the corresponding driver and hide mongotemplate op. The interface throws exception in case of null file, so i have to use the template to validate the delete whether was successful or not.
        File result = mongoTemplate.findById(createdFile.id, File.class);
        assertNull(result);
    }

    @Test
    public void getModifiedFiles() throws Exception {
        MockMultipartFile file = new MockMultipartFile("test", "test.png", "image/png", "TEST".getBytes());
        fileController.postFile(new CreateFileRequest("userid", 1L, 1L), file);

        MockMultipartFile file2 = new MockMultipartFile("test2", "test2.png", "image/png", "TEST2".getBytes());
        fileController.postFile(new CreateFileRequest("userid", 3L, 3L), file2);

        List<FileMetaInfo> result = fileController.getModifiedFiles(new GetModifiedFilesRequest("userid", 2L));

        FileMetaInfoMatcher expected = new FileMetaInfoMatcher()
                .relativePath("test2.png")
                .creationDateTime(3L)
                .modificationDateTime(3L)
                .length(5L);

        assertThat(result, Matchers.contains(expected));
    }

}
