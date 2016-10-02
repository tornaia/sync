package com.github.tornaia.sync.server.controller;

import com.github.tornaia.sync.server.data.document.File;
import com.github.tornaia.sync.server.data.repository.FileRepository;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import com.github.tornaia.sync.shared.util.FileSizeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Resource
    private FileRepository fileRepo;

    @RequestMapping(method = RequestMethod.POST)
    public FileMetaInfo postFile(@RequestParam("userid") String userid, @RequestPart("file") MultipartFile multipartFile) throws IOException {
        String path = multipartFile.getOriginalFilename();
        File file = fileRepo.findByPath(path);
        if (file == null) {
            file = fileRepo.insert(new File(path, multipartFile.getBytes(), userid, -1, -1));
            System.out.println("POST file: " + path + " (" + FileSizeUtils.toReadableFileSize(file.getData().length) + ")");
            return new FileMetaInfo(file.getId(), path, file.getData().length, file.getCreationDate(), file.getLastModifiedDate());
        }
        throw new FileAlreadyExistsException(path);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public FileMetaInfo putFile(@PathVariable String id, @RequestParam("userid") String userid, @RequestPart("file") MultipartFile multipartFile) throws IOException {
        String path = multipartFile.getOriginalFilename();
        File file = fileRepo.findOne(id);
        if (file == null) {
            throw new FileNotFoundException(path);
        } else {
            fileRepo.save(file);
        }
        System.out.println("PUT file: " + path + " (" + FileSizeUtils.toReadableFileSize(file.getData().length) + ")");
        return new FileMetaInfo(file.getId(), path, file.getData().length, file.getCreationDate(), file.getLastModifiedDate());
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void deleteFile(@PathVariable String id) throws IOException {
        File file = fileRepo.findOne(id);
        if (file == null) {
            throw new FileNotFoundException(id);
        }
        String path = file.getPath();
        fileRepo.delete(file);
        System.out.println("DELETE file: " + path);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM)
    public ResponseEntity getFile(@PathVariable String id, @RequestParam("userid") String userid) throws IOException {
        File file = fileRepo.findOne(id);
        if (file == null) {
            throw new FileNotFoundException(id);
        }
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
        System.out.println("GET file: " + file.getPath() + ")");
        return new ResponseEntity(file.getData(), responseHeaders, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}/metaInfo", method = RequestMethod.GET)
    public FileMetaInfo getMetaInfo(@PathVariable String id, @RequestParam("userid") String userid) throws IOException {
        File file = fileRepo.findOne(id);
        if (file == null) {
            throw new FileNotFoundException(id);
        }
        System.out.println("GET metaInfo: " + file.getPath() + " (" + FileSizeUtils.toReadableFileSize(file.getData().length) + ")");
        return new FileMetaInfo(file.getId(), file.getPath(), file.getData().length, file.getCreationDate(), file.getLastModifiedDate());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    class FileNotFoundException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public FileNotFoundException(String path) {
            super("Could not find file: '" + path + "'.");
        }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    class FileAlreadyExistsException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public FileAlreadyExistsException(String path) {
            super("File already exists: '" + path + "'.");
        }
    }
}
