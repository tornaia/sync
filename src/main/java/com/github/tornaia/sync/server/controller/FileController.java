package com.github.tornaia.sync.server.controller;

import com.github.tornaia.sync.server.data.document.File;
import com.github.tornaia.sync.server.service.FileCommandService;
import com.github.tornaia.sync.server.service.FileQueryService;
import com.github.tornaia.sync.server.service.exception.FileAlreadyExistsException;
import com.github.tornaia.sync.server.service.exception.FileNotFoundException;
import com.github.tornaia.sync.shared.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
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

    @RequestMapping(method = GET)
    public List<FileMetaInfo> getModifiedFiles(GetModifiedFilesRequest request) {
        return fileQueryService.getModifiedFiles(request.getUserid(), request.getModTs());
    }

    @RequestMapping(method = POST)
    public FileMetaInfo postFile(@RequestPart("fileAttributes") CreateFileRequest request, @RequestPart(value = "file", required = false) MultipartFile multipartFile, @RequestParam("clientid") String clientid) throws IOException {
        return fileCommandService.createFile(clientid, request.getUserid(), request.getSize(), request.getCreationDateTime(), request.getModificationDateTime(), request.getRelativePath(), multipartFile == null ? null : multipartFile.getInputStream());
    }

    @RequestMapping(value = "/{id}", method = PUT)
    public FileMetaInfo putFile(@PathVariable String id, @RequestPart("fileAttributes") UpdateFileRequest request, @RequestPart(value = "file", required = false) MultipartFile multipartFile, @RequestParam("clientid") String clientid) throws IOException {
        fileCommandService.modifyFile(clientid, id, request.getSize(), request.getCreationDateTime(), request.getModificationDateTime(), multipartFile == null ? null : multipartFile.getInputStream());
        return fileQueryService.getFileMetaInfoById(id);
    }

    @RequestMapping(value = "/delete/{id}", method = POST)
    public void deleteFile(@RequestPart("fileAttributes") DeleteFileRequest request, @RequestParam("clientid") String clientid) throws IOException {
        fileCommandService.deleteFile(clientid, request.getId(), request.getSize(), request.getCreationDateTime(), request.getModificationDateTime());
    }

    @RequestMapping(value = "/{id}", method = GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity getFile(@PathVariable String id) throws IOException {
        File file = fileQueryService.getFileById(id);
        long size = file.getSize();
        String filename = file.getFilename();
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(CONTENT_DISPOSITION, "attachment; filename=" + filename);
        LOG.info("GET file: " + file.getPath());
        InputStream content = fileQueryService.getContent(id);

        InputStreamResource inputStreamResource = new InputStreamResource(content);
        responseHeaders.setContentLength(size);
        return new ResponseEntity(inputStreamResource, responseHeaders, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}/metaInfo", method = GET)
    public FileMetaInfo getMetaInfo(@PathVariable String id) throws IOException {
        return fileQueryService.getFileMetaInfoById(id);
    }

    @ResponseStatus(value = NOT_FOUND, reason = "File was not found")
    @ExceptionHandler({FileNotFoundException.class})
    private void convertFileNotFoundExceptionTo404(FileNotFoundException e) {
    }

    @ResponseStatus(value = CONFLICT, reason = "File already exists")
    @ExceptionHandler({FileAlreadyExistsException.class})
    private void convertFileAlreadyExistsExceptionTo409(FileAlreadyExistsException e) {
    }
}
