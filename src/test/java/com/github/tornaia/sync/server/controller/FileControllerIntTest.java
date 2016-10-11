package com.github.tornaia.sync.server.controller;

import com.github.tornaia.sync.server.data.document.File;
import com.github.tornaia.sync.shared.api.*;
import com.github.tornaia.sync.shared.api.matchers.FileMetaInfoMatcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import utils.AbstractSyncServerIntTest;

import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
public class FileControllerIntTest extends AbstractSyncServerIntTest {

    @Autowired
    private FileController fileController;

    @Test
    public void getMetaInfo() throws Exception {
        MockMultipartFile file = new MockMultipartFile("test", "test.png", "image/png", "TEST".getBytes());
        CreateFileRequest createFileRequest = new CreateFileRequestBuilder()
                .userid("userid")
                .creationDateTime(1L)
                .modificationDateTime(2L)
                .create();
        fileController.postFile(createFileRequest, file, "clientid");

        List<FileMetaInfo> modifiedFiles = fileController.getModifiedFiles(new GetModifiedFilesRequestBuilder().userid("userid").modTS(-1L).create());
        FileMetaInfo result = fileController.getMetaInfo(modifiedFiles.get(0).id);

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
        CreateFileRequest createFileRequest = new CreateFileRequestBuilder()
                .userid("userid")
                .creationDateTime(2L)
                .modificationDateTime(3L)
                .create();
        fileController.postFile(createFileRequest, file, "clientid");

        List<FileMetaInfo> modifiedFiles = fileController.getModifiedFiles(new GetModifiedFilesRequestBuilder().userid("userid").modTS(-1L).create());
        FileMetaInfo fileMetaInfo = fileController.getMetaInfo(modifiedFiles.get(0).id);

        MockMultipartFile updatedFile = new MockMultipartFile("test", "test.png", "image/png", "TEST2".getBytes());

        UpdateFileRequest updateFileRequest = new UpdateFileRequestBuilder()
                .userid("userid")
                .creationDateTime(3L)
                .modificationDateTime(4L)
                .create();

        FileMetaInfo result = fileController.putFile(fileMetaInfo.id, updateFileRequest, updatedFile, "clientid");

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

        CreateFileRequest createFileRequest = new CreateFileRequestBuilder()
                .userid("userid")
                .creationDateTime(1L)
                .modificationDateTime(2L)
                .create();
        fileController.postFile(createFileRequest, file, "clientid");

        List<FileMetaInfo> modifiedFiles = fileController.getModifiedFiles(new GetModifiedFilesRequestBuilder().userid("userid").modTS(-1L).create());
        FileMetaInfo createdFile = fileController.getMetaInfo(modifiedFiles.get(0).id);

        fileController.deleteFile(createdFile.id, "clientid");

        //TODO nxjohny: We can wrap it in the corresponding driver and hide mongotemplate op. The interface throws exception in case of null file, so i have to use the template to validate the delete whether was successful or not.
        File result = mongoTemplate.findById(createdFile.id, File.class);
        assertNull(result);
    }

    @Test
    public void getModifiedFiles() throws Exception {
        MockMultipartFile file = new MockMultipartFile("test", "test.png", "image/png", "TEST".getBytes());
        CreateFileRequest createFileRequest = new CreateFileRequestBuilder()
                .userid("userid")
                .creationDateTime(1L)
                .modificationDateTime(1L)
                .create();
        fileController.postFile(createFileRequest, file, "clientid");

        MockMultipartFile file2 = new MockMultipartFile("test2", "test2.png", "image/png", "TEST2".getBytes());
        CreateFileRequest createFileRequest2 = new CreateFileRequestBuilder()
                .userid("userid")
                .creationDateTime(3L)
                .modificationDateTime(3L)
                .create();
        fileController.postFile(createFileRequest2, file2, "clientid");

        List<FileMetaInfo> result = fileController.getModifiedFiles(new GetModifiedFilesRequestBuilder().userid("userid").modTS(2L).create());

        FileMetaInfoMatcher expected = new FileMetaInfoMatcher()
                .relativePath("test2.png")
                .creationDateTime(3L)
                .modificationDateTime(3L)
                .length(5L);

        assertThat(result, contains(expected));
    }
}
