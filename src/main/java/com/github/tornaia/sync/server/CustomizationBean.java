package com.github.tornaia.sync.server;

import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.MultipartConfigElement;

@Configuration
public class CustomizationBean implements EmbeddedServletContainerCustomizer {

    // move these limits to shared module (client should not send file bigger than this)
    private static final int _5GB = 5 * 1073741824;
    private static final String _5120MB = "5120MB";

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
                tomcat.addConnectorCustomizers((connector) -> connector.setMaxPostSize(_5GB));
            }
        };
    }

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(_5120MB);
        factory.setMaxRequestSize(_5120MB);
        return factory.createMultipartConfig();
    }
}