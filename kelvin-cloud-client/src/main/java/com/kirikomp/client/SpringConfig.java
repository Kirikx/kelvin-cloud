package com.kirikomp.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.kirikomp.client")
public class SpringConfig {

    @Bean
    public NetConnection netConnection() {
        return new NetConnection();
    }

}
