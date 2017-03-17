package me.ohughes.config;

import lombok.extern.slf4j.Slf4j;
import me.ohughes.config.CompositeProperties.HeteroCompositeProperties;
import me.ohughes.config.CompositeProperties.PatternMatchingRepoProperties;
import me.ohughes.environment.HeteroCompositeEnvironmentRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.cloud.config.server.environment.EnvironmentWatch;
import org.springframework.cloud.config.server.environment.MultipleJGitEnvironmentRepository;
import org.springframework.cloud.config.server.environment.MultipleJGitEnvironmentRepository.PatternMatchingJGitEnvironmentRepository;
import org.springframework.cloud.config.server.environment.VaultEnvironmentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.beans.FeatureDescriptor;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Setup appropriate environment repositories based on what is listed in the composite config key
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(CompositeProperties.class)
public class HeteroCompositeConfig {

    private final CompositeProperties compositeProperties;

    @Autowired
    public HeteroCompositeConfig(CompositeProperties compositeProperties ) {
        this.compositeProperties = compositeProperties;
    }

    @PostConstruct
    public void init() {
        ExpressionParser spelParser = new SpelExpressionParser();
        Expression expression = spelParser.parseExpression("'${spring.cloud.config.server.composite:}'");
        for (HeteroCompositeProperties environment: compositeProperties.getComposite()) {
            log.info(environment.toString());

        }
    }
    @Bean
    @ConditionalOnProperty(prefix = "spring.cloud.config.server", value = "composite")
    public EnvironmentRepository masterCompositeRepository(List<EnvironmentRepository> compositeEnvironments){
        return new HeteroCompositeEnvironmentRepository(compositeEnvironments);
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.cloud.config.server", value = "composite")
    public List<EnvironmentRepository> compositeEnvironments(ConfigurableEnvironment environment, HttpServletRequest request){
        List<EnvironmentRepository> environments = new ArrayList<>();

        environments.addAll(compositeProperties.getComposite()
                .stream()
                .map(configProperties -> {
                    if(configProperties.getType().equals(EnvironmentType.GIT.getType())) {
                        return convertPropertiesToGitEnvironment(environment, configProperties);
                    }
                    if(configProperties.getType().equals(EnvironmentType.VAULT.getType())){
                        return convertPropertiesToVaultEnvironment(request, configProperties);
                    }
                    return null;

                })
                .collect(Collectors.toList()));

        return environments;
    }

    private EnvironmentRepository convertPropertiesToVaultEnvironment(HttpServletRequest request, HeteroCompositeProperties configProperties) {
        EnvironmentRepository vaultEnv = new VaultEnvironmentRepository(request, new EnvironmentWatch.Default(), new RestTemplate());
        BeanUtils.copyProperties(configProperties, vaultEnv, getNullPropertyNames(configProperties));
        return vaultEnv;
    }

    private EnvironmentRepository convertPropertiesToGitEnvironment(ConfigurableEnvironment environment, HeteroCompositeProperties configProperties) {
        MultipleJGitEnvironmentRepository multiGitEnv = new MultipleJGitEnvironmentRepository(environment);
        Map<String, PatternMatchingJGitEnvironmentRepository> convertedRepoMap = new LinkedHashMap<>();
        BeanUtils.copyProperties(configProperties, multiGitEnv, "repos");

        //Guard against null value for repos map
        Set<Entry<String, PatternMatchingRepoProperties>> extraRepoProperties = Optional
                .ofNullable(configProperties.getRepos())
                .map(Map::entrySet)
                .orElse(Collections.emptySet());

        //Map properties for each set of nested git repositories individually
        for (Entry<String, PatternMatchingRepoProperties> patternRepoEntry : extraRepoProperties){
            PatternMatchingJGitEnvironmentRepository patternMatchingRepo = new PatternMatchingJGitEnvironmentRepository();
            patternMatchingRepo.setName(patternRepoEntry.getKey());
            BeanUtils.copyProperties(patternRepoEntry.getValue(), patternMatchingRepo, getNullPropertyNames(patternRepoEntry.getValue()));
            convertedRepoMap.put(patternMatchingRepo.getName(), patternMatchingRepo);
        }
        multiGitEnv.setRepos(convertedRepoMap);
        return multiGitEnv;
    }

    private static String[] getNullPropertyNames (Object source) {
        final BeanWrapper wrappedSource = new BeanWrapperImpl(source);
        return Stream.of(wrappedSource.getPropertyDescriptors())
                .map(FeatureDescriptor::getName)
                .filter(propertyName -> wrappedSource.getPropertyValue(propertyName) == null)
                .toArray(String[]::new);
    }
}
