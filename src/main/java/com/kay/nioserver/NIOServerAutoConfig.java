package com.kay.nioserver;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:/nio-server.properties")
@EnableConfigurationProperties(NIOServerProperties.class)
public class NIOServerAutoConfig {
    @Bean
    public NIOServer nioServer(NIOServerProperties nioServerProperties) {
        return new NIOServerImpl(nioServerProperties);
    }
}
