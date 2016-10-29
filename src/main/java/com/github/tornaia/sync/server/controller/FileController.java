package com.github.tornaia.sync.server.controller;

import com.github.tornaia.sync.server.data.document.File;
import com.github.tornaia.sync.server.service.FileCommandService;
import com.github.tornaia.sync.server.service.FileQueryService;
import com.github.tornaia.sync.server.service.exception.DynamicStorageException;
import com.github.tornaia.sync.server.service.exception.FileAlreadyExistsException;
import com.github.tornaia.sync.server.service.exception.FileNotFoundException;
import com.github.tornaia.sync.server.service.exception.OutdatedException;
import com.github.tornaia.sync.shared.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tornaia.sync.shared.api.GetFileResponseStatus.FILE_STATUS_HEADER_FIELD_NAME;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.web.bind.annotation.RequestMethod.*;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final Logger LOG = LoggerFactory.getLogger(FileController.class);

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
            FileMetaInfo fileMetaInfo = fileCommandService.modifyFile(clientid, request.getUserid(), id, request.getSize(), request.getCreationDateTime(), request.getModificationDateTime(), multipartFile == null ? null : multipartFile.getInputStream());
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
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.add(CONTENT_DISPOSITION, "attachment; filename=" + file.getFilename());
            responseHeaders.setContentLength(file.getSize());
            responseHeaders.add(FILE_STATUS_HEADER_FIELD_NAME, GetFileResponseStatus.OK.name());
            return new ResponseEntity(inputStreamResource, responseHeaders, HttpStatus.OK);
        } catch (FileNotFoundException e) {
            return new ResponseEntity(createHeaderFor(GetFileResponseStatus.NOT_FOUND), HttpStatus.OK);
        } catch (DynamicStorageException e) {
            return new ResponseEntity(createHeaderFor(GetFileResponseStatus.TRANSFER_FAILED), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity(createHeaderFor(GetFileResponseStatus.UNKNOWN_PROBLEM), HttpStatus.OK);
        }
    }

    private static MultiValueMap<String, String> createHeaderFor(GetFileResponseStatus getFileResponseStatus) {
        Map<String, List<String>> httpHeaders = new HashMap<>();
        List<String> values = new ArrayList<>();
        values.add(getFileResponseStatus.name());
        httpHeaders.put(FILE_STATUS_HEADER_FIELD_NAME, values);
        return CollectionUtils.toMultiValueMap(httpHeaders);
    }
}
