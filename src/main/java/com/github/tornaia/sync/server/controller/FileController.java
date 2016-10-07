package com.github.tornaia.sync.server.controller;

import com.github.tornaia.sync.server.data.document.File;
import com.github.tornaia.sync.server.service.FileCommandService;
import com.github.tornaia.sync.server.service.FileQueryService;
import com.github.tornaia.sync.server.service.exception.FileAlreadyExistsException;
import com.github.tornaia.sync.server.service.exception.FileNotFoundException;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final Logger LOG = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private FileCommandService fileCommandService;

    @Autowired
    private FileQueryService fileQueryService;

    @RequestMapping(path = "/reset", method = RequestMethod.GET)
    public void resetDatabase() {
        fileCommandService.deleteAll();
    }

    @RequestMapping(method = RequestMethod.GET)
    public List<FileMetaInfo> getModifiedFiles(@RequestParam("userid") String userid, @RequestParam("modificationDateTime") long modTs) {
        return fileQueryService.getModifiedFiles(userid, modTs);
    }

    @RequestMapping(method = RequestMethod.POST)
    public void postFile(@RequestParam("userid") String userid, @RequestParam("creationDateTime") long creationDateTime, @RequestParam("modificationDateTime") long modificationDateTime, @RequestPart("file") MultipartFile multipartFile) throws IOException {
        fileCommandService.createFile(userid, creationDateTime, modificationDateTime, multipartFile.getOriginalFilename(), multipartFile.getBytes());
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public FileMetaInfo putFile(@PathVariable String id, @RequestParam("userid") String userid, @RequestParam("creationDateTime") long creationDateTime, @RequestParam("modificationDateTime") long modificationDateTime, @RequestPart("file") MultipartFile multipartFile) throws IOException {
        fileCommandService.updateFile(multipartFile.getOriginalFilename(), creationDateTime, modificationDateTime, multipartFile.getBytes());
        return fileQueryService.getFileMetaInfoByPath(multipartFile.getOriginalFilename());
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void deleteFile(@PathVariable String id) throws IOException {
        fileCommandService.deleteFile(id);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM)
    public ResponseEntity getFile(@PathVariable String id, @RequestParam("userid") String userid) throws IOException {
        File file = fileQueryService.getFileById(id);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
        LOG.info("GET file: " + file.getPath());
        return new ResponseEntity<>(file.getData(), responseHeaders, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}/metaInfo", method = RequestMethod.GET)
    public FileMetaInfo getMetaInfo(@PathVariable String id, @RequestParam("userid") String userid) throws IOException {
        return fileQueryService.getFileMetaInfoById(id);
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "File was not found")
    @ExceptionHandler({FileNotFoundException.class})
    private void fileNotFoundExceptionHandler() {
    }

    @ResponseStatus(value = HttpStatus.CONFLICT, reason = "File already exists")
    @ExceptionHandler({FileAlreadyExistsException.class})
    private void fileAlreadyExistsExceptionHandler() {
    }
}
