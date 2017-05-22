package me.ohughes.proxy;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import me.ohughes.composite.CompositeRepositoryConverter;
import me.ohughes.proxy.annotations.ProxyAwareRestBuilderQualifier;
import me.ohughes.proxy.annotations.ProxyAwareVaultQualifier;
import me.ohughes.proxy.properties.VaultProxyProperties;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.cloud.config.server.environment.EnvironmentWatch;
import org.springframework.cloud.config.server.environment.VaultEnvironmentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Configuration
@EnableConfigurationProperties(VaultProxyProperties.class)
@ConditionalOnExpression("'${spring.cloud.config.server.vault.proxy.http.host:}'!='' || '${spring.cloud.config.server.vault.proxy.https.host:}'!=''")
@Slf4j
public class VaultProxyEnvironmentConfiguration {

	/**
	 * {@link VaultEnvironmentRepository} does not expose it's underlying {@link RestTemplate} as a setter (only in constructor).
	 * For proxy servers to work correctly, the RestTemplate must be overridden to a version that has correct proxy configuration
	 */
	private final BaseProxyConfiguration baseProxyConfiguration;

	@Autowired
	public VaultProxyEnvironmentConfiguration(BaseProxyConfiguration baseProxyConfiguration) {
		this.baseProxyConfiguration = baseProxyConfiguration;
	}

	@Bean
	@ProxyAwareVaultQualifier
    RestTemplate proxyAwareRestTemplate(HttpClientBuilder builder, VaultProxyProperties proxyProperties) {
		//When no proxy settings are found, buildClient returns a plain instance of HttpClient
		//When proxy settings are defined, the HttpClient is setup to use proxy servers
		HttpClient httpClient = baseProxyConfiguration.buildClient(proxyProperties, builder);
		return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
	}

	/**
	 * Provide {@link } with a {@link RestTemplateBuilder}
	 * that is configured for using a proxy server
	 */
	@Bean
	@ProxyAwareRestBuilderQualifier
    RestTemplateBuilder proxyAwareRestTemplateBuilder(HttpClientBuilder builder, VaultProxyProperties proxyProperties,
                                                      ObjectProvider<HttpMessageConverters> messageConverters,
                                                      ObjectProvider<List<RestTemplateCustomizer>> restTemplateCustomizers) {

		log.debug("Using proxy configured RestTemplateBuilder for Vault Health Check Indicator");
		HttpClient httpClient = baseProxyConfiguration.buildClient(proxyProperties, builder);
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
		RestTemplateBuilder restTemplateBuilder = ProxyClientConfiguration.getRestTemplateBuilder(messageConverters, restTemplateCustomizers);
		return restTemplateBuilder.requestFactory(requestFactory);
	}

	@Configuration
	@Slf4j
	@ConditionalOnExpression("'${spring.cloud.config.server.vault.proxy.http.host:}'!='' || '${spring.cloud.config.server.vault.proxy.https.host:}'!=''")
	@EnableConfigurationProperties(OverrideVaultProperties.class)
	static class OverrideVaultEnvironment {
		/**
		 §* Setup a a {@link VaultEnvironmentRepository} that uses a proxy configured {@link RestTemplate}
		 */
		@Bean
		public EnvironmentRepository environmentRepository(@ProxyAwareVaultQualifier RestTemplate restTemplate, HttpServletRequest request,
                                                           OverrideVaultProperties vaultProperties, CompositeRepositoryConverter repositoryConverter) {

			log.info("Overriding Vault Envrionment {} to use proxy settings", vaultProperties);
			VaultEnvironmentRepository vaultEnvironmentRepository = new VaultEnvironmentRepository(request, new EnvironmentWatch.Default(), restTemplate);
			return new ProxyAwareVaultEnvironmentRepository(
				repositoryConverter.convertPropertiesToVaultEnvironment(vaultProperties, vaultEnvironmentRepository));
		}
	}

	/**
	 * Delegate for {@link VaultEnvironmentRepository that overrides Http proxy settings}
	 */
	static class ProxyAwareVaultEnvironmentRepository implements EnvironmentRepository, Ordered {

		private VaultEnvironmentRepository vaultEnvironmentRepository;

		ProxyAwareVaultEnvironmentRepository(VaultEnvironmentRepository vaultEnvironmentRepository) {
			this.vaultEnvironmentRepository = vaultEnvironmentRepository;
		}

		@Override
		public Environment findOne(String application, String profile, String label) {
			return vaultEnvironmentRepository.findOne(application, profile, label);
		}

		@Override
		public int getOrder(){
			//Higher precedence than parent Vault repo
			return vaultEnvironmentRepository.getOrder() - 1;
		}
	}

	@ConfigurationProperties("spring.cloud.config.server.vault")
	@Data
	static class OverrideVaultProperties {
		private String host;
		private int port;
		private String scheme;
		private String backend;
		private String defaultKey;
		private String profileSeparator;
	}
}

