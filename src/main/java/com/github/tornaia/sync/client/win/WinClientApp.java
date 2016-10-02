package com.github.tornaia.sync.client.win;

import com.github.tornaia.sync.client.win.websocket.WebSocketClientEndpoint;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.ws.rs.core.MediaType;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;

import static com.github.tornaia.sync.shared.util.FileSizeUtils.toReadableFileSize;
import static java.nio.file.StandardWatchEventKinds.*;

public class WinClientApp {

    private static final String SERVER_SCHEME = "http";
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 8080;
    private static final String SERVER_URL = SERVER_SCHEME + "://" + SERVER_HOST + ":" + SERVER_PORT;

    private static final String FILE_PATH = "/api/file";

    private static final String USERID = "7247234";

    private static final Path SYNC_DIRECTORY = FileSystems.getDefault().getPath("C:\\temp\\"); // "C:\\temp\\"

    private static final HttpClient HTTP_CLIENT = HttpClientBuilder.
            create()
            .disableAutomaticRetries()
            .addInterceptorFirst(new ThrowExceptionOnNonOkResponse())
            .build();

    private static final WatchService WATCHER;

    private static WebSocketClientEndpoint WEB_SOCKET_CLIENT_ENDPOINT;

    private static final File SYNC_CLIENT_STATE_FILE = new File("C:\\temp2\\sync-client-win.db");
    private static final SyncClientState SYNC_CLIENT_STATE;

    static {
        try {
            WATCHER = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            throw new RuntimeException("Cannot create watcher: ", e);
        }

        if (!SYNC_CLIENT_STATE_FILE.exists()) {
            SYNC_CLIENT_STATE = new SyncClientState();
        } else {
            try {
                FileInputStream fileInputStream = new FileInputStream(SYNC_CLIENT_STATE_FILE);
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                SYNC_CLIENT_STATE = (SyncClientState) objectInputStream.readObject();
                objectInputStream.close();

            } catch (ClassNotFoundException | IOException e) {
                throw new RuntimeException("Cannot read sync state file", e);
            }
        }

        writeSyncClientStateToDisk();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("PID: " + ManagementFactory.getRuntimeMXBean().getName());
        initWebSocketConnection();

        SYNC_DIRECTORY.register(WATCHER, new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW});
        registerRecursive(SYNC_DIRECTORY);

        while (true) {
            writeSyncClientStateToDisk();
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
                } else if (kind == ENTRY_MODIFY) {
                    onFileModify(filePath);
                } else if (kind == ENTRY_DELETE) {
                    onFileDelete(filePath);
                }

                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }

            Thread.yield();
        }
    }

    private static void initWebSocketConnection() {
        try {
            WEB_SOCKET_CLIENT_ENDPOINT = new WebSocketClientEndpoint(new URI("ws://127.0.0.1:8080/echo"));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        WEB_SOCKET_CLIENT_ENDPOINT.setMessageHandler(message -> System.out.println("WebSocketMessageHandler incoming message: " + message));
        WEB_SOCKET_CLIENT_ENDPOINT.sendMessage("{'sample-key-1':'sample-value-1','sample-key-2':'sample-value-2'}");
    }

    private static void writeSyncClientStateToDisk() {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(SYNC_CLIENT_STATE_FILE);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(SYNC_CLIENT_STATE);
            objectOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException("Cannot write state to disk", e);
        }
    }

    private static void onFileCreate(Path filePath) throws IOException {
        File file = SYNC_DIRECTORY.resolve(filePath).toFile();
        if (file.isFile()) {
            return;
        } else if (file.isDirectory()) {
            registerRecursive(filePath.toAbsolutePath());
        } else {
            System.out.println("Unknown file type. File does not exist? " + file);
        }
    }

    private static void onFileModify(Path filePath) throws IOException {
        File file = filePath.toFile();
        if (file.isDirectory()) {
            return;
        } else if (file.isFile()) {
            putFile(file);
        } else {
            System.out.println("Unknown file type. File does not exist? " + file);
        }
    }

    private static void onFileDelete(Path filePath) throws IOException {
        File file = SYNC_DIRECTORY.resolve(filePath).toFile();
        if (file.exists()) {
            System.out.println("File exist when we want to sync a delete event? " + file);
            return;
        }
        deleteObject(file);
    }

    private static void registerRecursive(Path root) {
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    System.out.println("WatchService registered for dir: " + dir.getFileName());
                    dir.register(WATCHER, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    System.out.println("VISIT FAILED: " + file + ", exception: " + exc.getMessage());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
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
        try {
            HTTP_CLIENT.execute(httpPut);
        } catch (FileNotFoundException e) {
            System.out.println("File disappeared? " + e.getMessage());
            return;
        }
        System.out.println("PUT file: " + relativePathWithinSyncFolder + " (" + toReadableFileSize(file.length()) + ")");
        SYNC_CLIENT_STATE.put(relativePathWithinSyncFolder, file);
    }

    private static void deleteObject(File object) throws IOException {
        String relativePathWithinSyncFolder = getRelativePath(object);

        URI uri;
        try {
            uri = new URIBuilder()
                    .setScheme(SERVER_SCHEME)
                    .setHost(SERVER_HOST)
                    .setPort(SERVER_PORT)
                    .setPath(FILE_PATH)
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

    private static String getRelativePath(File directory) {
        return directory.getAbsolutePath().substring(SYNC_DIRECTORY.toAbsolutePath().toFile().getAbsolutePath().length());
    }
}
