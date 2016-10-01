package com.github.tornaia.sync.server.file;

import com.github.tornaia.sync.shared.api.PutDirectoryRequest;
import com.github.tornaia.sync.shared.util.FileSizeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
public class FileController {

    @RequestMapping(value = "/api/file", method = RequestMethod.PUT)
    public void putFile(@RequestParam("userid") String userid, @RequestPart("file") MultipartFile multipartFile) throws IOException {
        String relativePathWithinSyncDir = multipartFile.getOriginalFilename();
        byte[] bytes = multipartFile.getBytes();
        System.out.println("PUT file: " + relativePathWithinSyncDir + " (" + FileSizeUtils.toReadableFileSize(bytes.length) + ")");
    }

    @RequestMapping(value = "/api/directory", method = RequestMethod.PUT)
    public void putDirectory(@RequestParam("userid") String userid, @RequestBody PutDirectoryRequest putDirectoryRequest) throws IOException {
        String relativePath = putDirectoryRequest.relativePath;
        System.out.println("PUT directory: " + relativePath);
    }
}
