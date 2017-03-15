package me.ohughes.config;

import lombok.extern.slf4j.Slf4j;
import me.ohughes.config.CompositeProperties.HeteroCompositeProperties;
import me.ohughes.environment.HeteroCompositeEnvironmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.config.server.environment.AbstractScmEnvironmentRepository;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
        for (HeteroCompositeProperties environment: compositeProperties.getComposite()) {
            log.info(environment.toString());

        }
    }

    @Bean
    public EnvironmentRepository masterCompositeRepository(){
        return new HeteroCompositeEnvironmentRepository();
    }

    @Bean
    public List<AbstractScmEnvironmentRepository> compositeGitEnvironments(){
        List<HeteroCompositeProperties> gitEnvironments = compositeProperties.getComposite()
                .stream()
                .filter(env -> Objects.equals(env.getType(), EnvironmentType.GIT.getType()))
                .collect(Collectors.toList());

        return null;
    }


    //TODO: for each environment in the list, create an environment repository of the given type

}
