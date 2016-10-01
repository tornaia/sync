package com.github.tornaia.sync.server.file;

import com.github.tornaia.sync.shared.AddFileRequest;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class FileController {

    private AtomicInteger counter = new AtomicInteger();

    @RequestMapping(value = "/api/file", method = RequestMethod.PUT)
    public void addFile(@RequestBody AddFileRequest addFileRequest) {
        return;
    }
}
