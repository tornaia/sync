package com.github.tornaia.sync.client.win.rest;

import com.github.tornaia.sync.client.win.rest.httpclient.FileCreateResponse;
import com.github.tornaia.sync.client.win.rest.httpclient.RecentChangesResponse;
import com.github.tornaia.sync.client.win.rest.httpclient.RestHttpClient;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class RestManager {

    @Autowired
    private RestHttpClient restHttpClient;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    public CompletableFuture<byte[]> getFile(FileMetaInfo fileMetaInfo) {
        return CompletableFuture.supplyAsync(() -> restHttpClient.getFile(fileMetaInfo), executorService);
    }

    public CompletableFuture<RecentChangesResponse> getAllAfter(long since) {
        return CompletableFuture.supplyAsync(() -> restHttpClient.getAllAfter(since), executorService);
    }

    public CompletableFuture<FileCreateResponse> onFileCreate(FileMetaInfo newFileMetaInfo, File file) {
        return CompletableFuture.supplyAsync(() -> restHttpClient.onFileCreate(newFileMetaInfo, file), executorService);
    }

    public CompletableFuture<FileCreateResponse> onFileModify(FileMetaInfo updatedFileMetaInfo, File file) {
        return CompletableFuture.supplyAsync(() -> restHttpClient.onFileModify(updatedFileMetaInfo, file), executorService);
    }

    public CompletableFuture<Void> onFileDelete(FileMetaInfo fileMetaInfo) {
        return CompletableFuture.runAsync(() -> restHttpClient.onFileDelete(fileMetaInfo), executorService);
    }
}
