package me.ohughes.environment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;

import java.util.*;
import java.util.Map.Entry;

/**
 * Master environment repository that searches for a config value
 * using all repositories in a composite list in order
 */
@Slf4j
@Data
@AllArgsConstructor
public class HeteroCompositeEnvironmentRepository implements EnvironmentRepository {

    private List<EnvironmentRepository> environmentRepositories;

    @Override
    public Environment findOne(String application, String profile, String label) {
        Environment compositeEnvironment = new Environment("composite", new String[]{profile}, label, null,null);

        Set<Object> foundKeys = new HashSet<>();
        for (EnvironmentRepository repo : environmentRepositories) {

            Environment env = repo.findOne(application, profile, label);
            //Each environment repository has a set of key/value properties. Deduplicate the property values
            //so only the first found is returned to the client.
            for(PropertySource source : env.getPropertySources()) {
                Map<Object, Object> newSourceMap = deduplicatePropertySource(foundKeys, source);
                compositeEnvironment.add(new PropertySource(source.getName(), newSourceMap));
            }
        }
        return compositeEnvironment;
    }

    private Map<Object, Object> deduplicatePropertySource(Set<Object> foundKeys, PropertySource source) {
        //Guard against null value for repos map
        Set<? extends Entry<?, ?>> sourceValueCollection = Optional
                .ofNullable(source.getSource())
                .map(Map::entrySet)
                .orElse(Collections.emptySet());

        Map<Object, Object> newSourceMap = new HashMap<>();
        for(Entry<?, ?> sourceValueEntry : sourceValueCollection){
            if(!foundKeys.contains(sourceValueEntry.getKey())){
                newSourceMap.put(sourceValueEntry.getKey(), sourceValueEntry.getValue());
                foundKeys.add(sourceValueEntry.getKey());
            }
        }
        return newSourceMap;
    }
}
