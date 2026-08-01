package com.cloudimny.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@SpringBootApplication
@EnableR2dbcRepositories
public class ApiApplication {

    static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }

    @Bean
    public DataBufferFactory dataBufferFactory() {
        return DefaultDataBufferFactory.sharedInstance;
    }
}
