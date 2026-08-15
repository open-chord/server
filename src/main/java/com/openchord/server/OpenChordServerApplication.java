package com.openchord.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Bootstraps the OpenChord backend and discovers its typed configuration properties.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
public class OpenChordServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenChordServerApplication.class, args);
    }
}
