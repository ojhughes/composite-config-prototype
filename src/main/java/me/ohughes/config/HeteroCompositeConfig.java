package me.ohughes.config;

import lombok.extern.slf4j.Slf4j;
import me.ohughes.config.CompositeProperties.HeteroCompositeProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * Setup appropriate environment repositories based on what is listed in the composite config key
 */
@Slf4j
@EnableConfigurationProperties(CompositeProperties.class)
@Configuration
public class HeteroCompositeConfig {

    private final CompositeProperties compositeProperties;

    @Autowired
    public HeteroCompositeConfig(CompositeProperties compositeProperties) {
        this.compositeProperties = compositeProperties;
    }

    @PostConstruct
    public void init() {
        for (HeteroCompositeProperties entry : compositeProperties.getComposite()) {
            log.info(entry.toString());
        }
    }


    //TODO: for each environment in the list, create an environment repository of the given type

}
