package com.github.tornaia.sync.server.controller;

import com.github.tornaia.sync.server.service.FileQueryService;
import com.github.tornaia.sync.shared.api.*;
import com.github.tornaia.sync.shared.api.matchers.FileMetaInfoMatcher;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import utils.AbstractSyncServerIntTest;

import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class FileControllerIntTest extends AbstractSyncServerIntTest {

    @Autowired
    private FileController fileController;

    @Autowired
    private FileQueryService fileQueryService;

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

        List<FileMetaInfo> modifiedFiles = fileQueryService.getModifiedFiles("userid", -1L);
        FileMetaInfo result = modifiedFiles.get(0);

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

        List<FileMetaInfo> modifiedFiles = fileQueryService.getModifiedFiles("userid", -1L);
        FileMetaInfo fileMetaInfo = modifiedFiles.get(0);

        MockMultipartFile updatedFile = new MockMultipartFile("test", "test.png", "image/png", "TEST2".getBytes());

        ModifyFileRequest modifyFileRequest = new ModifyFileRequestBuilder()
                .userid("userid")
                .size(4L)
                .creationDateTime(3L)
                .modificationDateTime(4L)
                .create();

        ResponseEntity<ModifyFileResponse> response = fileController.putFile(fileMetaInfo.id, modifyFileRequest, updatedFile, "clientid");
        ModifyFileResponse result = response.getBody();
        FileMetaInfo actual = result.fileMetaInfo;

        FileMetaInfoMatcher expectedMatcher = new FileMetaInfoMatcher()
                .relativePath("test.png")
                .size(4L)
                .creationDateTime(3L)
                .modificationDateTime(4L);

        assertThat(actual, expectedMatcher);
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

        List<FileMetaInfo> modifiedFiles = fileQueryService.getModifiedFiles("userid", -1L);
        FileMetaInfo createdFileMetaInfo = modifiedFiles.get(0);

        DeleteFileRequest deleteFileRequest = new DeleteFileRequestBuilder()
                .id(createdFileMetaInfo.id)
                .userid("userid")
                .size(4L)
                .creationDateTime(1L)
                .modificationDateTime(2L)
                .create();
        fileController.deleteFile(deleteFileRequest, "clientid");

        ResponseEntity<DeleteFileResponse> notFoundDeleteResponse = fileController.deleteFile(deleteFileRequest, "clientid");
        DeleteFileResponse body = notFoundDeleteResponse.getBody();
        assertEquals(DeleteFileResponse.Status.NOT_FOUND, body.status);
    }

    @Test
    public void deleteFileWhenRequestIsOutdated() throws Exception {
        MockMultipartFile file = new MockMultipartFile("test", "this.does.not.count.test.png", "image/png", "TEST".getBytes());

        CreateFileRequest createFileRequest = new CreateFileRequestBuilder()
                .userid("userid")
                .relativePath("test.png")
                .size(4L)
                .creationDateTime(1L)
                .modificationDateTime(2L)
                .create();
        fileController.postFile(createFileRequest, file, "clientid");

        List<FileMetaInfo> modifiedFiles = fileQueryService.getModifiedFiles("userid", -1L);
        FileMetaInfo createdFileMetaInfo = modifiedFiles.get(0);

        DeleteFileRequest deleteFileRequest = new DeleteFileRequestBuilder()
                .id(createdFileMetaInfo.id)
                .userid("userid")
                .size(5L)
                .creationDateTime(1L)
                .modificationDateTime(3L)
                .create();

        ResponseEntity<DeleteFileResponse> outdatedDeleteResponse = fileController.deleteFile(deleteFileRequest, "clientid");

        DeleteFileResponse body = outdatedDeleteResponse.getBody();
        assertEquals(DeleteFileResponse.Status.OUTDATED, body.status);
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

        List<FileMetaInfo> createdFiles = fileQueryService.getModifiedFiles("userid", 2L);

        FileMetaInfoMatcher expected = new FileMetaInfoMatcher()
                .relativePath("test2.png")
                .size(5L)
                .creationDateTime(3L)
                .modificationDateTime(3L);

        assertThat(createdFiles, contains(expected));
    }
}