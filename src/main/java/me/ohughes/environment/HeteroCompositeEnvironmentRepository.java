package me.ohughes.environment;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * Master environment repository that searches for a config value
 * using all repositories in a composite list in order
 */
@Slf4j
@Data
public class HeteroCompositeEnvironmentRepository implements EnvironmentRepository {

    @Resource
    private List<EnvironmentRepository> environmentRepositories;

    @Override
    public Environment findOne(String application, String profile, String label) {
        Environment compositeEnvironment = new Environment("composite", new String[]{profile}, label, null,null);
        for (EnvironmentRepository repo : environmentRepositories) {

            Environment source = repo.findOne(application, profile, label);
            source.getPropertySources().forEach(compositeEnvironment::add);
        }
        return compositeEnvironment;
    }
}
