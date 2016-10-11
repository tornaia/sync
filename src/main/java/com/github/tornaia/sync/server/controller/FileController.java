package com.github.tornaia.sync.server.controller;

import com.github.tornaia.sync.server.data.document.File;
import com.github.tornaia.sync.server.service.FileCommandService;
import com.github.tornaia.sync.server.service.FileQueryService;
import com.github.tornaia.sync.server.service.exception.FileAlreadyExistsException;
import com.github.tornaia.sync.server.service.exception.FileNotFoundException;
import com.github.tornaia.sync.shared.api.CreateFileRequest;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import com.github.tornaia.sync.shared.api.GetModifiedFilesRequest;
import com.github.tornaia.sync.shared.api.UpdateFileRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpStatus.*;
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
    public List<FileMetaInfo> getModifiedFiles(@RequestBody GetModifiedFilesRequest request) {
        return fileQueryService.getModifiedFiles(request.getUserid(), request.getModTs());
    }

    @RequestMapping(method = POST)
    public FileMetaInfo postFile(@RequestPart("fileAttributes") CreateFileRequest request, @RequestPart("file") MultipartFile multipartFile, @RequestParam("clientid") String clientid) throws IOException {
        return fileCommandService.createFile(clientid, request.getUserid(), request.getCreationDateTime(), request.getModificationDateTime(), multipartFile.getOriginalFilename(), multipartFile.getBytes());
    }

    @RequestMapping(value = "/{id}", method = PUT)
    public FileMetaInfo putFile(@PathVariable String id, @RequestPart("fileAttributes") UpdateFileRequest request, @RequestPart("file") MultipartFile multipartFile, @RequestParam("clientid") String clientid) throws IOException {
        fileCommandService.updateFile(clientid, id, request.getCreationDateTime(), request.getModificationDateTime(), multipartFile.getBytes());
        return fileQueryService.getFileMetaInfoById(id);
    }

    @RequestMapping(value = "/{id}", method = DELETE)
    public void deleteFile(@PathVariable String id, @RequestParam("clientid") String clientid) throws IOException {
        fileCommandService.deleteFile(clientid, id);
    }

    @RequestMapping(value = "/{id}", method = GET, produces = APPLICATION_OCTET_STREAM)
    public ResponseEntity getFile(@PathVariable String id) throws IOException {
        File file = fileQueryService.getFileById(id);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
        LOG.info("GET file: " + file.getPath());
        return new ResponseEntity<>(file.getData(), responseHeaders, OK);
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
