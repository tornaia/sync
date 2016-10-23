package com.github.tornaia.sync.e2e;

import com.github.tornaia.sync.client.win.WinClientApp;
import com.github.tornaia.sync.client.win.httpclient.FailOnErrorResponseInterceptor;
import com.github.tornaia.sync.server.ServerApp;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(SpringRunner.class)
public abstract class AbstractIntTest {

    protected static final int REPEAT = 1;

    private static final Logger LOG = LoggerFactory.getLogger(AbstractIntTest.class);

    private static final AtomicInteger clientIdGenerator = new AtomicInteger();

    @Value("#{systemProperties['sync.backend.server.scheme'] ?: 'http'}")
    private String serverScheme;

    @Value("#{systemProperties['sync.backend.server.host'] ?: '127.0.0.1'}")
    private String serverHost;

    @Value("#{systemProperties['sync.backend.server.port'] ?: '8080'}")
    private int serverPort;

    @Value("#{systemProperties['clients.root.path'] ?: 'C:\\temp\\e2etests\\\\'}")
    protected String clientsRootDirectoryPath;

    private ConfigurableApplicationContext server;

    private List<Client> clients = new ArrayList<>();

    @Before
    public void init() throws Exception {
        startServer();
        resetDB();
    }

    @After
    public void closeServer() {
        LOG.info("Shutdown server");
        server.close();
    }

    @After
    public void closeClients() {
        LOG.info("Shutdown all clients");
        clients.stream().forEach(client -> client.close());
    }

    protected void startServer() {
        server = new SpringApplicationBuilder(ServerApp.class)
                .profiles("real-mongo-local", "jubos-s3-faker")
                .headless(false)
                .run();
    }

    protected void resetDB() throws Exception {
        if (server == null) {
            throw new IllegalStateException("Server not running!");
        }

        Thread thread = new Thread(() ->
        {
            try {
                HttpClient httpClient = HttpClientBuilder.
                        create()
                        .disableAutomaticRetries()
                        .addInterceptorFirst(new FailOnErrorResponseInterceptor())
                        .build();

                URI uri = new URIBuilder()
                        .setScheme(serverScheme)
                        .setHost(serverHost)
                        .setPort(serverPort)
                        .setPath("/api/files/reset")
                        .build();

                HttpGet httpGet = new HttpGet(uri);

                httpClient.execute(httpGet);
                LOG.info("DB reset done");
            } catch (URISyntaxException | IOException e) {
                throw new RuntimeException(e);
            }
        });
        thread.start();
        thread.join();
    }

    public class Client {

        public String userid;
        public Path syncDirectory;
        private ConfigurableApplicationContext appContext;

        public Client(String userid, Path syncDirectory) {
            this.userid = userid;
            this.syncDirectory = syncDirectory;
        }

        public Client start() {
            appContext = new SpringApplicationBuilder(WinClientApp.class)
                    .web(false)
                    .headless(false)
                    .profiles("localhost")
                    .run("--client.sync.directory.path=" + syncDirectory.toFile().getAbsolutePath(),
                            "--client.sync.userid=" + userid,
                            "--logging.file=" + syncDirectory.getParent().resolve("logs").resolve(syncDirectory.getFileName()));

            clients.add(this);
            LOG.info("Start client for userid: " + userid + ", syncDirectory: " + syncDirectory.toFile().getAbsolutePath());
            return this;
        }

        public Client close() {
            LOG.info("Close client for userid: " + userid + ", syncDirectory: " + syncDirectory.toFile().getAbsolutePath());
            appContext.close();
            return this;
        }
    }

    protected Client initClient(String userid) {
        int id = clientIdGenerator.incrementAndGet();
        Path syncDirectory = new File(clientsRootDirectoryPath).toPath().resolve("client" + id);
        createEmptyDirectory(syncDirectory);
        return new Client(userid, syncDirectory);
    }

    protected void createDirectory(Path path, long creationTime, long lastModifiedTime) throws IOException {
        String tmpDir = System.getProperty("java.io.tmpdir");
        Path tempDirectory = Files.createDirectory(new File(tmpDir).toPath().resolve(UUID.randomUUID().toString()));

        setCreationTimeAndModifiedTime(tempDirectory, creationTime, lastModifiedTime);

        Files.move(tempDirectory, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    protected void createFile(Path path, String content, long creationTime, long lastModifiedTime) throws IOException {
        String tmpDir = System.getProperty("java.io.tmpdir");
        Path tempFile = Files.createFile(new File(tmpDir).toPath().resolve(UUID.randomUUID().toString()));
        try (FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
            IOUtils.write(content, fos);
        }

        setCreationTimeAndModifiedTime(tempFile, creationTime, lastModifiedTime);

        path.toFile().getParentFile().mkdirs();
        Files.move(tempFile, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    protected void setCreationTimeAndModifiedTime(Path path, long creationTime, long lastModifiedTime) throws IOException {
        setCreationTime(path, creationTime);
        setLastModifiedTime(path, lastModifiedTime);
    }

    protected void deleteDirectory(Path path) throws IOException {
        FileUtils.forceDelete(path.toFile());
    }

    protected void deleteFile(Path path) {
        path.toFile().delete();
    }

    protected void waitForSyncDone() {
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createEmptyDirectory(Path path) {
        File clientsRootDirectory = path.toFile();
        try {
            FileUtils.deleteDirectory(clientsRootDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create an empty for client", e);
        }
        clientsRootDirectory.mkdirs();
    }

    private static void setCreationTime(Path path, long creationDateTime) throws IOException {
        Files.setAttribute(path, "basic:creationTime", FileTime.fromMillis(creationDateTime));
    }

    private static void setLastModifiedTime(Path path, long modificationDateTime) throws IOException {
        Files.setAttribute(path, "basic:lastModifiedTime", FileTime.fromMillis(modificationDateTime));
    }
}
