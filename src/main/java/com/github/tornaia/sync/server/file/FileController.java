package com.github.tornaia.sync.server.file;

import com.github.tornaia.sync.shared.api.DeleteFileRequest;
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

    @RequestMapping(value = "/api/file", method = RequestMethod.DELETE)
    public void deleteDirectory(DeleteFileRequest deleteFileRequest) throws IOException {
        String userid = deleteFileRequest.getUserid();
        String relativePath = deleteFileRequest.getRelativePath();
        System.out.println("DELETE file: " + relativePath);
    }
}
