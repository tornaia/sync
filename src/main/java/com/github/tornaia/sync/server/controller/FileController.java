package com.github.tornaia.sync.server.controller;

import com.github.tornaia.sync.server.data.document.File;
import com.github.tornaia.sync.server.service.FileCommandService;
import com.github.tornaia.sync.server.service.FileQueryService;
import com.github.tornaia.sync.server.service.exception.DynamicStorageException;
import com.github.tornaia.sync.server.service.exception.FileAlreadyExistsException;
import com.github.tornaia.sync.server.service.exception.FileNotFoundException;
import com.github.tornaia.sync.server.service.exception.OutdatedException;
import com.github.tornaia.sync.shared.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

import static com.github.tornaia.sync.shared.api.GetFileResponseStatus.FILE_STATUS_HEADER_FIELD_NAME;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.web.bind.annotation.RequestMethod.*;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private FileCommandService fileCommandService;

    @Autowired
    private FileQueryService fileQueryService;

    @RequestMapping(path = "/reset", method = GET)
    public void resetDatabase() {
        fileCommandService.deleteAll();
    }

    @RequestMapping(method = POST)
    public ResponseEntity<CreateFileResponse> postFile(@RequestPart("fileAttributes") CreateFileRequest request, @RequestPart(value = "file", required = false) MultipartFile multipartFile, @RequestParam("clientid") String clientid) throws IOException {
        try {
            FileMetaInfo fileMetaInfo = fileCommandService.createFile(clientid, request.getUserid(), request.getSize(), request.getCreationDateTime(), request.getModificationDateTime(), request.getRelativePath(), multipartFile == null ? null : multipartFile.getInputStream());
            return ResponseEntity.ok(CreateFileResponse.ok(fileMetaInfo));
        } catch (FileAlreadyExistsException e) {
            return ResponseEntity.ok(CreateFileResponse.alreadyExists(e.getMessage()));
        } catch (DynamicStorageException e) {
            return ResponseEntity.ok(CreateFileResponse.transferFailed(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(CreateFileResponse.unknownProblem());
        }
    }

    @RequestMapping(value = "/{id}", method = PUT)
    public ResponseEntity<ModifyFileResponse> putFile(@PathVariable String id, @RequestPart("fileAttributes") ModifyFileRequest request, @RequestPart(value = "file", required = false) MultipartFile multipartFile, @RequestParam("clientid") String clientid) throws IOException {
        try {
            FileMetaInfo fileMetaInfo = fileCommandService.modifyFile(clientid, request.getUserid(), id, request.getOldSize(), request.getOldCreationDateTime(), request.getOldModificationDateTime(), request.getNewSize(), request.getNewCreationDateTime(), request.getNewModificationDateTime(), multipartFile == null ? null : multipartFile.getInputStream());
            return ResponseEntity.ok(ModifyFileResponse.ok(fileMetaInfo));
        } catch (FileNotFoundException e) {
            return ResponseEntity.ok(ModifyFileResponse.notFound(e.getMessage()));
        } catch (OutdatedException e) {
            return ResponseEntity.ok(ModifyFileResponse.outdated(e.getMessage()));
        } catch (DynamicStorageException e) {
            return ResponseEntity.ok(ModifyFileResponse.transferFailed(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(ModifyFileResponse.unknownProblem());
        }
    }

    @RequestMapping(value = "/delete/{id}", method = POST)
    public ResponseEntity<DeleteFileResponse> deleteFile(@RequestPart("fileAttributes") DeleteFileRequest request, @RequestParam("clientid") String clientid) throws IOException {
        try {
            fileCommandService.deleteFile(clientid, request.getUserid(), request.getId(), request.getSize(), request.getCreationDateTime(), request.getModificationDateTime());
            return ResponseEntity.ok(DeleteFileResponse.ok());
        } catch (FileNotFoundException e) {
            return ResponseEntity.ok(DeleteFileResponse.notFound(e.getMessage()));
        } catch (OutdatedException e) {
            return ResponseEntity.ok(DeleteFileResponse.outdated(e.getMessage()));
        } catch (DynamicStorageException e) {
            return ResponseEntity.ok(DeleteFileResponse.transferFailed(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(DeleteFileResponse.unknownProblem());
        }
    }

    @RequestMapping(value = "/{id}", method = GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity getFile(@PathVariable String id, @RequestParam("userid") String userid) {
        try {
            File file = fileQueryService.getFileById(userid, id);
            InputStream content = fileQueryService.getContent(id);
            InputStreamResource inputStreamResource = new InputStreamResource(content);
            HttpHeaders responseHeaders = createHeaderFor(GetFileResponseStatus.OK);
            responseHeaders.add(CONTENT_DISPOSITION, "attachment; filename=" + file.getFilename());
            responseHeaders.setContentLength(file.getSize());
            return ResponseEntity.ok().headers(responseHeaders).body(inputStreamResource);
        } catch (FileNotFoundException e) {
            return ResponseEntity.ok().headers(createHeaderFor(GetFileResponseStatus.NOT_FOUND)).build();
        } catch (DynamicStorageException e) {
            return ResponseEntity.ok().headers(createHeaderFor(GetFileResponseStatus.TRANSFER_FAILED)).build();
        } catch (Exception e) {
            return ResponseEntity.ok().headers(createHeaderFor(GetFileResponseStatus.UNKNOWN_PROBLEM)).build();
        }
    }

    private static HttpHeaders createHeaderFor(GetFileResponseStatus getFileResponseStatus) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(FILE_STATUS_HEADER_FIELD_NAME, getFileResponseStatus.name());
        return httpHeaders;
    }
}
