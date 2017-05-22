package me.ohughes.proxy;

import me.ohughes.proxy.properties.BaseProxyProperties;
import me.ohughes.proxy.properties.HttpProxyProperties;
import me.ohughes.proxy.properties.HttpsProxyProperties;
import me.ohughes.proxy.properties.ProxyHostProperties;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;

import java.util.Arrays;
import java.util.Set;

import static org.springframework.util.StringUtils.hasText;

/**
 * Static utilities for handling proxy properties
 */
class Proxies {

	static CredentialsProvider buildCredentialsProvider(ProxyHostProperties proxyProperties) {
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
        UsernamePasswordCredentials userPasswordCreds = new UsernamePasswordCredentials(proxyProperties.getUsername(), proxyProperties.getPassword());
        credsProvider.setCredentials(new AuthScope(proxyProperties.getHost(), proxyProperties.getPort()), userPasswordCreds);
		return credsProvider;
	}

	static CredentialsProvider buildCredentialsProvider(Set<ProxyHostProperties> proxyPropertySet) {
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		for (ProxyHostProperties proxyProperties : proxyPropertySet) {
			UsernamePasswordCredentials userPasswordCreds = new UsernamePasswordCredentials(proxyProperties.getUsername(), proxyProperties.getPassword());
			credsProvider.setCredentials(new AuthScope(proxyProperties.getHost(), proxyProperties.getPort()), userPasswordCreds);
		}
		return credsProvider;
	}

	static ProxyHostProperties coalesce(ProxyHostProperties... proxyProperties) {
		return Arrays.stream(proxyProperties)
			.filter(Proxies::hasProxyProperties)
			.findFirst()
			.orElse(new ProxyHostProperties());
	}

	static boolean hasProxyCredentials(ProxyHostProperties proxyProperties) {
		return hasText(proxyProperties.getUsername()) && hasText(proxyProperties.getPassword());
	}

	static boolean hasHttpNonProxyHosts(BaseProxyProperties properties) {
		HttpProxyProperties httpProperties = properties.getHttp();
		return httpProperties != null && hasText(httpProperties.getNonProxyHosts());
	}

	static boolean hasHttpsNonProxyHosts(BaseProxyProperties properties){
		HttpsProxyProperties httpsProperties = properties.getHttps();
		return httpsProperties != null && hasText(httpsProperties.getNonProxyHosts());
	}

	static boolean hasProxyProperties(ProxyHostProperties proxyProperties) {
		return proxyProperties != null &&
			hasText(proxyProperties.getHost()) &&
			proxyProperties.getPort() > 0;
	}
}
