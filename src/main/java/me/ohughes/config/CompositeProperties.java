package me.ohughes.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Container for composite property structure
 */
@Data
@ConfigurationProperties("spring.cloud.config.server")
public class CompositeProperties {

    private List<HeteroCompositeProperties> composite;

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class HeteroCompositeProperties extends GitRepoProperties {
        private String type;
        private String host;
        private String scheme;
        private int port;
        private String backend;
        private String profileSeparator;
        private String defaultKey;
        private Map<String, PatternMatchingRepoProperties> repos = new LinkedHashMap<>();
    }

    @Data
    public static class GitRepoProperties{
        private boolean cloneOnStart;
        private boolean forcePull;
        private String uri;
        private String username;
        private String password;
        private String[] searchPaths = new String[0];
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class PatternMatchingRepoProperties extends GitRepoProperties {
        private String[] pattern = new String[0];
        private String name;
    }
}
