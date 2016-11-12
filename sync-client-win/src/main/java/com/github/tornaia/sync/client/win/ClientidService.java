package com.github.tornaia.sync.client.win;

import com.github.tornaia.sync.client.win.util.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ClientidService {

    private static final Logger LOG = LoggerFactory.getLogger(ClientidService.class);

    public final String clientid;

    @Autowired
    public ClientidService(RandomUtils randomUtils) {
        clientid = randomUtils.getRandomString();
        LOG.debug("Clientid: " + clientid);
    }
}
