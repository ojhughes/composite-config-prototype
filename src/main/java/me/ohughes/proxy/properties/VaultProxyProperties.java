package me.ohughes.proxy.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spring.cloud.config.server.vault.proxy")
public class VaultProxyProperties extends BaseProxyProperties {
}
