package com.github.tornaia.sync.server;

import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.stereotype.Component;

@Component
public class CustomizationBean implements EmbeddedServletContainerCustomizer {

    @Override
    public void customize(ConfigurableEmbeddedServletContainer container) {
        String port = System.getenv("PORT");
        if (port == null) {
            port = "8080";
        }
        container.setPort(Integer.parseInt(port));
    }
}