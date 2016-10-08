package com.github.tornaia.sync.e2e;

import com.github.tornaia.sync.client.win.WinClientApp;
import com.github.tornaia.sync.client.win.httpclient.FailOnErrorResponseInterceptor;
import com.github.tornaia.sync.server.ServerApp;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.assertj.core.util.Files;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.UUID;

import static java.nio.file.Files.createTempFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
public class WinClientAndServerIntTest {

    private static final Logger LOG = LoggerFactory.getLogger(WinClientAndServerIntTest.class);

    // TODO these properties are ugly.. go with a property file or whatever
    // later this tests should run on jenkins, other op systems or
    // we should be able to run our test suit against target environments

    private static final long userid = System.currentTimeMillis();

    private static final String client1SyncDirectoryPath = "C:\\temp\\client-1\\";
    private static final String client1StateFilePath = "C:\\temp2\\client-1.db";

    private static final String client2SyncDirectoryPath = "C:\\temp\\client-2\\";
    private static final String client2StateFilePath = "C:\\temp2\\client-2.db";


    @Value("#{systemProperties['server.scheme.http'] ?: 'http'}")
    private String serverSchemeHttp;

    @Value("#{systemProperties['server.host'] ?: '127.0.0.1'}")
    private String serverHost;

    @Value("#{systemProperties['server.port'] ?: '8080'}")
    private int serverPort;

    private ConfigurableApplicationContext server;
    private ConfigurableApplicationContext winClient1;
    private ConfigurableApplicationContext winClient2;

    @Before
    public void initServerWith2Clients() throws Exception {
        Files.delete(new File(client1SyncDirectoryPath));
        Files.delete(new File(client2SyncDirectoryPath));
        Files.delete(new File(client1StateFilePath));
        Files.delete(new File(client2StateFilePath));

        server = new SpringApplicationBuilder(ServerApp.class).headless(false).run();

        resetDB();

        winClient1 = new SpringApplicationBuilder(WinClientApp.class)
                .web(false)
                .headless(false)
                .run("--client.sync.directory.path=" + client1SyncDirectoryPath,
                        "--client.state.file.path=" + client1StateFilePath,
                        "--frosch-sync.userid=" + userid);

        winClient2 = new SpringApplicationBuilder(WinClientApp.class)
                .web(false)
                .headless(false)
                .run("--client.sync.directory.path=" + client2SyncDirectoryPath,
                        "--client.state.file.path=" + client2StateFilePath,
                        "--frosch-sync.userid=" + userid);
    }

    private void resetDB() throws Exception {
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

    // TODO super ugly I know but I want to get a failing (then passing) test ASAP
    @Test
    public void bothCanStart() throws Exception {
        Thread.sleep(1000L);

        assertTrue(new File(client1SyncDirectoryPath).list().length == 0);

        Path tempFile = createTempFile(UUID.randomUUID().toString(), "suffix");
        try (FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
            IOUtils.write("dummy content", fos);
        }

        java.nio.file.Files.setAttribute(tempFile, "basic:lastModifiedTime", FileTime.fromMillis(600L));
        java.nio.file.Files.setAttribute(tempFile, "basic:creationTime", FileTime.fromMillis(500L));
        Path targetFile = new File(client1SyncDirectoryPath).toPath().resolve("dummy.txt");
        java.nio.file.Files.move(tempFile, targetFile, StandardCopyOption.ATOMIC_MOVE);


        // wait for sync
        Thread.sleep(5000L);
        server.close();
        winClient1.close();
        winClient2.close();

        assertTrue(new File(client1SyncDirectoryPath).list().length == 1);
        assertTrue(new File(client2SyncDirectoryPath).list().length == 1);
        assertEquals("dummy content", IOUtils.toString(new FileInputStream(new File(client1SyncDirectoryPath + "/dummy.txt"))));
    }
}
