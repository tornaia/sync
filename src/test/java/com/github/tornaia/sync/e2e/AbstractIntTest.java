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
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(SpringRunner.class)
public abstract class AbstractIntTest {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractIntTest.class);

    private static final AtomicInteger clientIdGenerator = new AtomicInteger();

    @Value("#{systemProperties['server.scheme.http'] ?: 'http'}")
    private String serverSchemeHttp;

    @Value("#{systemProperties['server.host'] ?: '127.0.0.1'}")
    private String serverHost;

    @Value("#{systemProperties['server.port'] ?: '8080'}")
    private int serverPort;

    protected final String clientsRootDirectoryPath = "C:\\temp\\e2etests\\";

    protected ConfigurableApplicationContext server;

    protected void startServer() {
        if (!Objects.isNull(server)) {
            throw new IllegalStateException("Server already started!");
        }
        server = new SpringApplicationBuilder(ServerApp.class).headless(false).run();
    }

    protected void resetDB() throws Exception {
        if (Objects.isNull(server)) {
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
                        .setScheme(serverSchemeHttp)
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

    protected Path startNewClient(String userid) {
        int id = clientIdGenerator.incrementAndGet();
        Path clientSyncDirectory = new File(clientsRootDirectoryPath).toPath().resolve("client" + id);
        createEmptyDirectory(clientSyncDirectory);
        new SpringApplicationBuilder(WinClientApp.class)
                .web(false)
                .headless(false)
                .run("--client.sync.directory.path=" + clientSyncDirectory.toFile().getAbsolutePath(),
                        "--frosch-sync.userid=" + userid);

        return clientSyncDirectory;
    }

    protected void createFile(Path path, String content, FileTime creationTime, FileTime lastModifiedTime) throws IOException {
        Path tempFile = Files.createTempFile(UUID.randomUUID().toString(), "suffix");
        try (FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
            IOUtils.write(content, fos);
        }

        Files.setAttribute(tempFile, "basic:creationTime", creationTime);
        Files.setAttribute(tempFile, "basic:lastModifiedTime", lastModifiedTime);
        Files.move(tempFile, path, StandardCopyOption.ATOMIC_MOVE);
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
}
