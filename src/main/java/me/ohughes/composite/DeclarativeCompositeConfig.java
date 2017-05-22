package me.ohughes.composite;

import lombok.extern.slf4j.Slf4j;
import me.ohughes.proxy.BaseProxyConfiguration;
import me.ohughes.proxy.ProxyAwareHttpClientFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.jgit.transport.HttpTransport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.cloud.config.server.environment.EnvironmentWatch;
import org.springframework.cloud.config.server.environment.MultipleJGitEnvironmentRepository;
import org.springframework.cloud.config.server.environment.VaultEnvironmentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Setup appropriate environment repositories based on what is listed in the composite config key
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(CompositeProperties.class)
@ConditionalOnProperty("spring.cloud.config.server.composite[0].type")
@EnableConfigServer
public class DeclarativeCompositeConfig {

	private final CompositeProperties compositeProperties;
	private CompositeRepositoryConverter propertiesConverter;
	private BaseProxyConfiguration baseProxyConfiguration;
	private HttpClientBuilder httpClientBuilder;


	@Autowired
	public DeclarativeCompositeConfig(CompositeProperties compositeProperties, CompositeRepositoryConverter propertiesConverter,
									  BaseProxyConfiguration baseProxyConfiguration, HttpClientBuilder httpClientBuilder) {

		this.compositeProperties = compositeProperties;
		this.propertiesConverter = propertiesConverter;
		this.baseProxyConfiguration = baseProxyConfiguration;
		this.httpClientBuilder = httpClientBuilder;
	}

	@Bean
	@ConditionalOnProperty("spring.cloud.config.server.composite[0].type")
	public EnvironmentRepository declarativeCompositeRepository(ConfigurableEnvironment environment, HttpServletRequest request) {
		List<DeclarativeCompositeHolder> environments = new ArrayList<>();
		environments.addAll(compositeProperties.getComposite()
			.stream()
			.map(configProperties -> {

				if (configProperties.getType().equals(EnvironmentType.GIT.getType())) {
					DeclarativeCompositeHolder environmentHolder = new DeclarativeCompositeHolder();
					HttpTransport.setConnectionFactory(new ProxyAwareHttpClientFactory(baseProxyConfiguration.buildClient(configProperties.getProxy(), httpClientBuilder)));
					MultipleJGitEnvironmentRepository gitEnvironmentRepository = propertiesConverter.convertPropertiesToGitEnvironment(environment, configProperties);
					environmentHolder.setEnvironmentRepository(gitEnvironmentRepository);

					return environmentHolder;
				}
				if (configProperties.getType().equals(EnvironmentType.VAULT.getType())) {
					DeclarativeCompositeHolder environmentHolder = new DeclarativeCompositeHolder();
					HttpClient proxyAwareClient = baseProxyConfiguration.buildClient(configProperties.getProxy(), httpClientBuilder);
					RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(proxyAwareClient));
					environmentHolder.setEnvironmentRepository(propertiesConverter.convertPropertiesToVaultEnvironment(
						configProperties, new VaultEnvironmentRepository(request, new EnvironmentWatch.Default(), restTemplate)));
					return environmentHolder;
				}
				return null;

			})
			.collect(Collectors.toList()));

		return new DeclarativeCompositeEnvironmentRepository(environments);
	}

	@PostConstruct
	public void init() {
		for (CompositeProperties.DeclarativeCompositeProperties environment : compositeProperties.getComposite()) {
			log.info(environment.toString());

		}
	}

	public enum EnvironmentType {
		GIT("git"),
		VAULT("vault");

		private final String type;

		EnvironmentType(String type) {
			this.type = type;
		}

		public String getType() {
			return type;
		}
	}

}
