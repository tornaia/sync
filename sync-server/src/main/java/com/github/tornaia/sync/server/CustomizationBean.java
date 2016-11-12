package com.github.tornaia.sync.server;

import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.MultipartConfigElement;

import static com.github.tornaia.sync.shared.constant.FileSizeConstants.MAX_SYNC_FILE_SIZE_IN_BYTES;
import static com.github.tornaia.sync.shared.constant.FileSizeConstants.MAX_SYNC_FILE_SIZE_IN_MEGABYTES;

@Configuration
public class CustomizationBean implements EmbeddedServletContainerCustomizer {

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
                tomcat.addConnectorCustomizers((connector) -> connector.setMaxPostSize(MAX_SYNC_FILE_SIZE_IN_BYTES));
            }
        };
    }

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(MAX_SYNC_FILE_SIZE_IN_MEGABYTES);
        factory.setMaxRequestSize(MAX_SYNC_FILE_SIZE_IN_MEGABYTES);
        return factory.createMultipartConfig();
    }
}