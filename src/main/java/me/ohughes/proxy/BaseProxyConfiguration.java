package me.ohughes.proxy;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.ohughes.proxy.properties.BaseProxyProperties;
import me.ohughes.proxy.properties.HttpProxyProperties;
import me.ohughes.proxy.properties.HttpsProxyProperties;
import me.ohughes.proxy.properties.ProxyHostProperties;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.apache.http.protocol.HttpContext;
import org.eclipse.jgit.transport.HttpTransport;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ProxySelector;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.springframework.util.StringUtils.hasText;

/**
 * Common functionality for configuring config server HTTP(S) proxy support
 */
@Slf4j
@AllArgsConstructor
@Component
public class BaseProxyConfiguration {

	private static final String HTTP_NON_PROXY_HOSTS_KEY = "http.nonProxyHosts";
	private static final String HTTPS_NON_PROXY_HOSTS_KEY = "https.nonProxyHosts";
	private static final String HTTP_PROXY_HOST_KEY = "http.proxyHost";
	private static final String HTTP_PROXY_PORT_KEY = "http.proxyPort";
	private static final String HTTPS_PROXY_HOST_KEY = "https.proxyHost";
	private static final String HTTPS_PROXY_PORT_KEY = "https.proxyPort";

	/**
	 * @return Either a proxy configured {@link HttpClient} or a default instance
	 */
	public HttpClient buildClient(BaseProxyProperties proxyProperties, HttpClientBuilder httpClientBuilder) {
		if (proxyProperties != null) {
			HttpsProxyProperties httpsProperties = proxyProperties.getHttps();
			HttpProxyProperties httpProperties = proxyProperties.getHttp();
			if (Proxies.hasProxyProperties(httpsProperties) || Proxies.hasProxyProperties(httpProperties)) {

                return buildClient(httpProperties, httpsProperties, httpClientBuilder);
            }
		}
//		RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(500).setConnectTimeout(500).setSocketTimeout(500).build();

		return httpClientBuilder.setConnectionTimeToLive(500, TimeUnit.MILLISECONDS).build();

	}

	void configureJgitProxy(BaseProxyProperties gitProxyProperties, HttpClientBuilder httpClientBuilder) {
		setNonProxyHostProperty(gitProxyProperties);
		// Setup HttpClient to use configured proxy servers and override JGits default client
		HttpClient httpClient = buildClient(gitProxyProperties, httpClientBuilder);
		HttpTransport.setConnectionFactory(new ProxyAwareHttpClientFactory(httpClient));
	}
	/**
	 * When multiple proxies are defined, set proxy host details as System properties and the route planner will pick these
	 * and determine the proxy to use based on the scheme of the request URI
	 */
	void clientWithMultipleProxies(HttpProxyProperties httpProperties, HttpsProxyProperties httpsProperties, HttpClientBuilder httpClientBuilder) {
		setSystemProxyHostProperties(httpProperties, httpsProperties);
		httpClientBuilder.setRoutePlanner(new SystemDefaultRoutePlanner(ProxySelector.getDefault()));

		//Set credentials for each proxy (The AuthScope uses host + port as a key, the realm defaults to `ANY`)
		Set<ProxyHostProperties> credentialSet = new HashSet<>();
		for (ProxyHostProperties proxyHostProperties : Arrays.asList(httpProperties, httpsProperties)) {
			if (Proxies.hasProxyCredentials(proxyHostProperties)) {
				credentialSet.add(proxyHostProperties);
				CredentialsProvider multiScopeCredentials = Proxies.buildCredentialsProvider(credentialSet);
				httpClientBuilder.setDefaultCredentialsProvider(multiScopeCredentials);
			}
		}
		log.info("Configuring client to use HTTP proxy {} and HTTPS proxy {}", httpProperties, httpsProperties);
	}

	/**
	 * When setting a single proxy, simply use {@link HttpClientBuilder} setProxy() method
	 */
	void clientWithSingleProxy(HttpProxyProperties httpProperties, HttpsProxyProperties httpsProperties, HttpClientBuilder httpClientBuilder) {
		ProxyHostProperties populatedProperties = Proxies.coalesce(httpProperties, httpsProperties);
		httpClientBuilder.setProxy(new HttpHost(populatedProperties.getHost(), populatedProperties.getPort()));
		configureProxyCredentials(populatedProperties, httpClientBuilder);
		log.info("Configuring client to use proxy {}", populatedProperties);
	}

	private void configureProxyCredentials(ProxyHostProperties proxyProperties, HttpClientBuilder clientBuilder) {
		if (Proxies.hasProxyCredentials(proxyProperties)) {
			CredentialsProvider credsProvider = Proxies.buildCredentialsProvider(proxyProperties);
			clientBuilder.setDefaultCredentialsProvider(credsProvider);
		}
	}

	void setNonProxyHostProperty(BaseProxyProperties properties) {
		if (Proxies.hasHttpNonProxyHosts(properties)) {
			setSystemProperty(HTTP_NON_PROXY_HOSTS_KEY, properties.getHttp().getNonProxyHosts());
		}
		// Depending on JVM, http.nonProxyHosts will be used for both HTTP and HTTPS requests.
		// If only HTTPS set, then set http.nonProxyHosts and https.nonProxyHosts
		if (Proxies.hasHttpsNonProxyHosts(properties) && !Proxies.hasHttpNonProxyHosts(properties)) {
			setSystemProperty(HTTP_NON_PROXY_HOSTS_KEY, properties.getHttps().getNonProxyHosts());
			setSystemProperty(HTTPS_NON_PROXY_HOSTS_KEY, properties.getHttps().getNonProxyHosts());
		} else if (Proxies.hasHttpsNonProxyHosts(properties)) {
			setSystemProperty(HTTPS_NON_PROXY_HOSTS_KEY, properties.getHttps().getNonProxyHosts());
		}
	}

	private HttpClient buildClient(HttpProxyProperties httpProperties, HttpsProxyProperties httpsProperties, HttpClientBuilder httpClientBuilder) {
		httpClientBuilder.useSystemProperties();

		boolean hasBothHttpAndHttpsProxySettings = Stream.of(httpProperties, httpsProperties)
			.allMatch(Proxies::hasProxyProperties);

		if (hasBothHttpAndHttpsProxySettings) {

			clientWithMultipleProxies(httpProperties, httpsProperties, httpClientBuilder);
		} else {
			clientWithSingleProxy(httpProperties, httpsProperties, httpClientBuilder);
		}
		httpClientBuilder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
		return httpClientBuilder.build();
	}

	private void setSystemProperty(String key, String value) {
		if (hasText(value)) {
			System.setProperty(key, value);
		}
	}

	private void setSystemProxyHostProperties(HttpProxyProperties httpProperties, HttpsProxyProperties httpsProperties) {
		setSystemProperty(HTTP_PROXY_HOST_KEY, httpProperties.getHost());
		setSystemProperty(HTTP_PROXY_PORT_KEY, String.valueOf(httpProperties.getPort()));
		setSystemProperty(HTTPS_PROXY_HOST_KEY, httpsProperties.getHost());
		setSystemProperty(HTTPS_PROXY_PORT_KEY, String.valueOf(httpsProperties.getPort()));
	}


}
