package com.github.tornaia.sync.server;

import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class CustomizationBean implements EmbeddedServletContainerCustomizer {

    private static final int _1GB = 1073741824;

    @Override
    public void customize(ConfigurableEmbeddedServletContainer container) {
        String port = System.getenv("PORT");
        if (port == null) {
            port = "8080";
        }
        container.setPort(Integer.parseInt(port));
    }

    @Bean
    public EmbeddedServletContainerCustomizer containerCustomizer() {
        return (ConfigurableEmbeddedServletContainer container) -> {
            if (container instanceof TomcatEmbeddedServletContainerFactory) {
                TomcatEmbeddedServletContainerFactory tomcat = (TomcatEmbeddedServletContainerFactory) container;
                tomcat.addConnectorCustomizers((connector) -> connector.setMaxPostSize(_1GB));
            }
        };
    }
}