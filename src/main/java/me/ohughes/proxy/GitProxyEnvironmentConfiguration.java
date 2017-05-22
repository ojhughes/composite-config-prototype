package me.ohughes.proxy;

import lombok.extern.slf4j.Slf4j;
import me.ohughes.proxy.annotations.ProxyAwareRestBuilderQualifier;
import me.ohughes.proxy.properties.GitProxyProperties;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.jgit.transport.HttpTransport;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Setup Http clients used by Git backed config servers to enable proxy server access
 */
@Configuration
@EnableConfigurationProperties(GitProxyProperties.class)
@ConditionalOnExpression("'${spring.cloud.config.server.git.proxy.http.host:}'!='' || '${spring.cloud.config.server.git.proxy.https.host:}'!=''")
@Slf4j
public class GitProxyEnvironmentConfiguration extends WebMvcConfigurerAdapter {

	private final GitProxyProperties proxyProperties;
	private final HttpClientBuilder httpClientBuilder;
	private final BaseProxyConfiguration baseProxyConfiguration;

	@Autowired
	public GitProxyEnvironmentConfiguration(GitProxyProperties proxyProperties, HttpClientBuilder httpClientBuilder, BaseProxyConfiguration baseProxyConfiguration) {
		this.proxyProperties = proxyProperties;
		this.httpClientBuilder = httpClientBuilder;
		this.baseProxyConfiguration = baseProxyConfiguration;
	}

	@PostConstruct
	public void configureProxySettings() {
		baseProxyConfiguration.setNonProxyHostProperty(proxyProperties);
		// Setup HttpClient to use configured proxy servers and override JGits default client
		HttpClient httpClient = baseProxyConfiguration.buildClient(proxyProperties, httpClientBuilder);
		HttpTransport.setConnectionFactory(new ProxyAwareHttpClientFactory(httpClient));
	}

	/**
	 * Provide  with a {@link RestTemplateBuilder}
	 * that is configured for using a proxy server
	 */
	@Bean
	@ProxyAwareRestBuilderQualifier
    RestTemplateBuilder proxyAwareRestTemplateBuilder(HttpClientBuilder builder, GitProxyProperties proxyProperties,
                                                      ObjectProvider<HttpMessageConverters> messageConverters,
                                                      ObjectProvider<List<RestTemplateCustomizer>> restTemplateCustomizers) {

		log.debug("Using proxy configured RestTemplateBuilder for Git Health Check Indicator");
		HttpClient httpClient = baseProxyConfiguration.buildClient(proxyProperties, builder);
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
		RestTemplateBuilder restTemplateBuilder = ProxyClientConfiguration.getRestTemplateBuilder(messageConverters, restTemplateCustomizers);
		return restTemplateBuilder.requestFactory(requestFactory);
	}
}
