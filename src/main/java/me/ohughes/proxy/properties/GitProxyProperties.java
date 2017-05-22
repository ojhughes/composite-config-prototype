package me.ohughes.proxy.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spring.cloud.config.server.git.proxy")
public class GitProxyProperties extends BaseProxyProperties {
}
