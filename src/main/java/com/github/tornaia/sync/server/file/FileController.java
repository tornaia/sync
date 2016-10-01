package com.github.tornaia.sync.server.file;

import com.github.tornaia.sync.util.FileSizeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class FileController {

    private AtomicInteger counter = new AtomicInteger();

    @RequestMapping(value = "/api/file", method = RequestMethod.PUT)
    public void addFile(@RequestParam("userid") String userid, @RequestPart("file") MultipartFile multipartFile) throws IOException {
        String relativePathWithinSyncDir = multipartFile.getOriginalFilename();
        byte[] bytes = multipartFile.getBytes();
        System.out.println("File received: " + relativePathWithinSyncDir + " (" + FileSizeUtils.toReadableFileSize(bytes.length) + ")");
        return;
    }
}
