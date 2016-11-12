package com.github.tornaia.sync.client.win.util;

import com.github.tornaia.sync.client.win.ClientidService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class ClientSettings {

    private static final Logger LOG = LoggerFactory.getLogger(ClientSettings.class);

    @Value("${client.sync.userid}")
    private String userid;

    @Value("${client.sync.directory.path}")
    private String directoryPath;

    @Value("${logging.file}")
    private String loggingFile;

    @Value("${sync.backend.server.scheme}")
    private String backendServerScheme;

    @Value("${sync.backend.server.host}")
    private String backendServerHost;

    @Value("${sync.backend.server.port}")
    private int backendServerPort;

    @Value("${sync.pusher.server.scheme}")
    private String pusherServerScheme;

    @Value("${sync.pusher.server.host}")
    private String pusherServerHost;

    @Value("${sync.pusher.server.port}")
    private int pusherServerPort;

    @Value("${sync.pusher.server.path}")
    private String pusherServerPath;

    @Autowired
    private ClientidService clientidService;

    @PostConstruct
    public void printInfo() {
        LOG.info("Userid: " + userid);
        LOG.info("Clientid: " + clientidService.clientid);
        LOG.info("DirectoryPath: " + directoryPath);
        LOG.info("LoggingFile: " + loggingFile);
        LOG.info("BackendServerScheme: " + backendServerScheme);
        LOG.info("BackendServerHost: " + backendServerHost);
        LOG.info("BackendServerPort: " + backendServerPort);
        LOG.info("PusherServerScheme: " + pusherServerScheme);
        LOG.info("PusherServerHost: " + pusherServerHost);
        LOG.info("PusherServerPort: " + pusherServerPort);
        LOG.info("PusherServerPath: " + pusherServerPath);
    }
}
