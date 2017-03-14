package me.ohughes.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * Setup appropriate environment repositories based on what is listed in the composite config key
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties("spring.cloud.config.server")
public class HeteroCompositeConfig {

    private List<CompositeProperties> composite;

    @PostConstruct
    public void init() {
        for (CompositeProperties entry : composite) {
            log.info(entry.toString());
        }
    }

    @Data
    public static class CompositeProperties{
        private String type;
        private String uri;
        private String host;
        private String scheme;
        private int port;

    }

    //TODO: for each environment in the list, create an environment repository of the given type

}
