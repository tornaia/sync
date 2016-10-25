package com.github.tornaia.sync.server.controller;

import com.github.tornaia.sync.server.service.exception.FileNotFoundException;
import com.github.tornaia.sync.shared.api.*;
import com.github.tornaia.sync.shared.api.matchers.FileMetaInfoMatcher;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import utils.AbstractSyncServerIntTest;

import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

public class FileControllerIntTest extends AbstractSyncServerIntTest {

    @Autowired
    private FileController fileController;

    @Test
    public void getMetaInfo() throws Exception {
        MockMultipartFile file = new MockMultipartFile("test", "this.does.not.count.test.png", "image/png", "TEST".getBytes());
        CreateFileRequest createFileRequest = new CreateFileRequestBuilder()
                .userid("userid")
                .relativePath("test.png")
                .size(4L)
                .creationDateTime(1L)
                .modificationDateTime(2L)
                .create();
        fileController.postFile(createFileRequest, file, "clientid");

        List<FileMetaInfo> modifiedFiles = fileController.getModifiedFiles(new GetModifiedFilesRequestBuilder().userid("userid").modTS(-1L).create());
        FileMetaInfo result = fileController.getMetaInfo(modifiedFiles.get(0).id, "userid");

        FileMetaInfoMatcher expected = new FileMetaInfoMatcher()
                .relativePath("test.png")
                .creationDateTime(1L)
                .modificationDateTime(2L)
                .size(4L);

        assertThat(result, expected);
    }

    @Test
    public void putFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("test", "this.does.not.count.test.png", "image/png", "TEST".getBytes());
        CreateFileRequest createFileRequest = new CreateFileRequestBuilder()
                .userid("userid")
                .relativePath("test.png")
                .size(4L)
                .creationDateTime(2L)
                .modificationDateTime(3L)
                .create();
        fileController.postFile(createFileRequest, file, "clientid");

        List<FileMetaInfo> modifiedFiles = fileController.getModifiedFiles(new GetModifiedFilesRequestBuilder().userid("userid").modTS(-1L).create());
        FileMetaInfo fileMetaInfo = fileController.getMetaInfo(modifiedFiles.get(0).id, "userid");

        MockMultipartFile updatedFile = new MockMultipartFile("test", "test.png", "image/png", "TEST2".getBytes());

        UpdateFileRequest updateFileRequest = new UpdateFileRequestBuilder()
                .userid("userid")
                .size(4L)
                .creationDateTime(3L)
                .modificationDateTime(4L)
                .create();

        FileMetaInfo result = fileController.putFile(fileMetaInfo.id, updateFileRequest, updatedFile, "clientid");

        FileMetaInfoMatcher expected = new FileMetaInfoMatcher()
                .relativePath("test.png")
                .size(4L)
                .creationDateTime(3L)
                .modificationDateTime(4L);

        assertThat(result, expected);
    }

    @Test
    public void deleteFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("test", "this.does.not.count.test.png", "image/png", "TEST".getBytes());

        CreateFileRequest createFileRequest = new CreateFileRequestBuilder()
                .userid("userid")
                .relativePath("test.png")
                .size(4L)
                .creationDateTime(1L)
                .modificationDateTime(2L)
                .create();
        fileController.postFile(createFileRequest, file, "clientid");

        List<FileMetaInfo> modifiedFiles = fileController.getModifiedFiles(new GetModifiedFilesRequestBuilder().userid("userid").modTS(-1L).create());
        FileMetaInfo createdFile = fileController.getMetaInfo(modifiedFiles.get(0).id, "userid");

        DeleteFileRequest deleteFileRequest = new DeleteFileRequestBuilder()
                .id(createdFile.id)
                .userid("userid")
                .size(4L)
                .creationDateTime(1L)
                .modificationDateTime(2L)
                .create();
        fileController.deleteFile(deleteFileRequest, "clientid");

        expectedException.expect(FileNotFoundException.class);
        fileController.getMetaInfo("userid", createdFile.id);
    }

    @Test
    public void deleteFileWhenRequesterIsInAsync() throws Exception {
        MockMultipartFile file = new MockMultipartFile("test", "this.does.not.count.test.png", "image/png", "TEST".getBytes());

        CreateFileRequest createFileRequest = new CreateFileRequestBuilder()
                .userid("userid")
                .relativePath("test.png")
                .size(4L)
                .creationDateTime(1L)
                .modificationDateTime(2L)
                .create();
        fileController.postFile(createFileRequest, file, "clientid");

        List<FileMetaInfo> modifiedFiles = fileController.getModifiedFiles(new GetModifiedFilesRequestBuilder().userid("userid").modTS(-1L).create());
        FileMetaInfo createdFile = fileController.getMetaInfo(modifiedFiles.get(0).id, "userid");

        DeleteFileRequest deleteFileRequest = new DeleteFileRequestBuilder()
                .id(createdFile.id)
                .userid("userid")
                .size(5L)
                .creationDateTime(1L)
                .modificationDateTime(3L)
                .create();
        expectedException.expect(FileNotFoundException.class);
        fileController.deleteFile(deleteFileRequest, "clientid");
    }

    @Test
    public void getModifiedFiles() throws Exception {
        MockMultipartFile file = new MockMultipartFile("test", "this.does.not.count.test.png", "image/png", "TEST".getBytes());
        CreateFileRequest createFileRequest = new CreateFileRequestBuilder()
                .userid("userid")
                .relativePath("test.png")
                .size(4L)
                .creationDateTime(1L)
                .modificationDateTime(1L)
                .create();
        fileController.postFile(createFileRequest, file, "clientid");

        MockMultipartFile file2 = new MockMultipartFile("test2", "this.does.not.count.test2.png", "image/png", "TEST2".getBytes());
        CreateFileRequest createFileRequest2 = new CreateFileRequestBuilder()
                .userid("userid")
                .relativePath("test2.png")
                .size(5L)
                .creationDateTime(3L)
                .modificationDateTime(3L)
                .create();
        fileController.postFile(createFileRequest2, file2, "clientid");

        List<FileMetaInfo> result = fileController.getModifiedFiles(new GetModifiedFilesRequestBuilder().userid("userid").modTS(2L).create());

        FileMetaInfoMatcher expected = new FileMetaInfoMatcher()
                .relativePath("test2.png")
                .size(5L)
                .creationDateTime(3L)
                .modificationDateTime(3L);

        assertThat(result, contains(expected));
    }
}