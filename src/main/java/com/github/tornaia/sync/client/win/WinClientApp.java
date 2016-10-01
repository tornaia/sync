package com.github.tornaia.sync.client.win;

import com.github.tornaia.sync.shared.api.PutDirectoryRequest;
import com.sun.nio.file.SensitivityWatchEventModifier;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static com.github.tornaia.sync.client.win.util.SerializerUtils.toStringEntity;
import static com.github.tornaia.sync.shared.util.FileSizeUtils.toReadableFileSize;
import static java.nio.file.StandardWatchEventKinds.*;

public class WinClientApp {

    private static final String SERVER_SCHEME = "http";
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 8080;
    private static final String SERVER_URL = SERVER_SCHEME + "://" + SERVER_HOST + ":" + SERVER_PORT;

    private static final String HELLO_PATH = "/api/hello";
    private static final String FILE_PATH = "/api/file";
    private static final String DIRECTORY_PATH = "/api/directory";
    private static final String OBJECT_PATH = "/api/object";

    private static final String USERID = "7247234";

    private static final Path SYNC_DIRECTORY = FileSystems.getDefault().getPath("C:\\temp\\");

    private static final HttpClient HTTP_CLIENT = HttpClientBuilder.
            create()
            .addInterceptorFirst(new ThrowExceptionOnNonOkResponse())
            .build();

    private static final WatchService WATCHER;

    static {
        try {
            WATCHER = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            throw new RuntimeException("Cannot create watcher: ", e);
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("PID: " + ManagementFactory.getRuntimeMXBean().getName());
        String serverInfo = getServerInfo();
        System.out.println("Server says: " + serverInfo);

        SYNC_DIRECTORY.register(WATCHER, new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW}, SensitivityWatchEventModifier.HIGH);
        registerRecursive(WATCHER, SYNC_DIRECTORY);

        while (true) {
            WatchKey key;
            try {
                key = WATCHER.poll(25, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                return;
            }
            if (key == null) {
                Thread.yield();
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path filename = ev.context();

                if (kind == OVERFLOW) {
                    continue;
                }

                Path ownerPath = (Path) key.watchable();
                Path filePath = ownerPath.resolve(filename);
                if (kind == ENTRY_CREATE) {
                    onFileCreate(filePath);
                } else if (kind == ENTRY_DELETE) {
                    onFileDelete(filePath);
                } else if (kind == ENTRY_MODIFY) {
                    onFileModify(filePath);
                }

                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }

            Thread.yield();
        }
    }

    private static void registerRecursive(WatchService watchService, Path root) {
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    System.out.println("WatchService registered for dir: " + dir.getFileName());
                    dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void onFileCreate(Path filePath) throws IOException {
        File file = SYNC_DIRECTORY.resolve(filePath).toFile();
        if (file.isFile()) {
            putFile(file);
        } else if (file.isDirectory()) {
            putDirectory(file);
            registerRecursive(WATCHER, filePath.toAbsolutePath());
        } else {
            throw new IllegalStateException("Unknown file type: " + file);
        }
    }


    private static void putFile(File file) throws IOException {
        String relativePathWithinSyncFolder = getRelativePath(file);

        HttpEntity multipart = MultipartEntityBuilder
                .create()
                .addBinaryBody("file", file, ContentType.APPLICATION_OCTET_STREAM, relativePathWithinSyncFolder)
                .build();

        HttpPut httpPut = new HttpPut(SERVER_URL + FILE_PATH + "?userid=" + USERID);
        httpPut.setEntity(multipart);
        HTTP_CLIENT.execute(httpPut);
        System.out.println("PUT file: " + relativePathWithinSyncFolder + " (" + toReadableFileSize(file.length()) + ")");
    }

    private static void putDirectory(File directory) throws IOException {
        String relativePathWithinSyncFolder = getRelativePath(directory);

        HttpPut httpPut = new HttpPut(SERVER_URL + DIRECTORY_PATH + "?userid=" + USERID);
        PutDirectoryRequest putDirectoryRequest = new PutDirectoryRequest(relativePathWithinSyncFolder);
        httpPut.setEntity(toStringEntity(putDirectoryRequest));
        httpPut.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        HTTP_CLIENT.execute(httpPut);
        System.out.println("PUT directory: " + relativePathWithinSyncFolder);
    }

    private static void onFileDelete(Path filePath) throws IOException {
        File file = SYNC_DIRECTORY.resolve(filePath).toFile();
        if (file.exists()) {
            throw new IllegalStateException("Object must not exist on delete: " + file);
        }
        delete(file);
    }

    private static void delete(File object) throws IOException {
        String relativePathWithinSyncFolder = getRelativePath(object);

        URI uri;
        try {
            uri = new URIBuilder()
                    .setScheme(SERVER_SCHEME)
                    .setHost(SERVER_HOST)
                    .setPort(SERVER_PORT)
                    .setPath(OBJECT_PATH)
                    .addParameter("userid", USERID)
                    .addParameter("relativePath", relativePathWithinSyncFolder)
                    .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        HttpDelete httpDelete = new HttpDelete(uri);
        httpDelete.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);

        HTTP_CLIENT.execute(httpDelete);
        System.out.println("DELETE file: " + relativePathWithinSyncFolder);
    }

    private static void onFileModify(Path filePath) throws IOException {
        File file = filePath.toFile();
        if (file.isDirectory()) {
            return;
        } else if (file.isFile()) {
            onFileCreate(filePath);
        } else {
            throw new IllegalStateException("Unknown file: " + file);
        }
    }

    private static String getServerInfo() throws IOException {
        HttpResponse response = HTTP_CLIENT.execute(new HttpGet(SERVER_URL + HELLO_PATH));
        return IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
    }

    private static String getRelativePath(File directory) {
        return directory.getAbsolutePath().substring(SYNC_DIRECTORY.toAbsolutePath().toFile().getAbsolutePath().length());
    }
}
