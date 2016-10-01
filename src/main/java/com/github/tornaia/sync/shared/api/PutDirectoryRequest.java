package com.github.tornaia.sync.shared.api;

public class PutDirectoryRequest {

    public String relativePath;

    public PutDirectoryRequest() {
        // must have because of serialization
    }

    public PutDirectoryRequest(String relativePath) {
        this.relativePath = relativePath;
    }
}
