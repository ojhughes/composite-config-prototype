package me.ohughes.config;

import org.eclipse.jgit.util.CachedAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.config.server.config.ConfigServerConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

@Configuration
@AutoConfigureBefore(ConfigServerConfiguration.class)
@EnableConfigurationProperties(GitProxyProperties.class)
public class GitProxyConfiguration {

	private static final Logger logger = LoggerFactory
			.getLogger(GitProxyConfiguration.class);

	private static final String HTTP_PROXY_HOST_KEY = "http.proxyHost";

	private static final String HTTP_PROXY_PORT_KEY = "http.proxyPort";

	private static final String HTTP_PROXY_USER_KEY = "http.proxyUser";

	private static final String HTTP_PROXY_PASS_KEY = "http.proxyPassword";

	private static final String HTTPS_PROXY_HOST_KEY = "https.proxyHost";

	private static final String HTTPS_PROXY_PORT_KEY = "https.proxyPort";

	private static final String HTTPS_PROXY_USER_KEY = "https.proxyUser";

	private static final String HTTPS_PROXY_PASS_KEY = "https.proxyPassword";

	private static final String HTTP_NON_PROXY_HOSTS_KEY = "http.nonProxyHosts";

	@Autowired
	private GitProxyProperties properties;

	@PostConstruct
	public void configureSystemProperties() {

		if (this.properties == null) {
			return;
		}

		GitProxyProperties.Http httpProperties = this.properties.getHttp();
		GitProxyProperties.Https httpsProperties = this.properties.getHttps();

		if (httpProperties != null) {
			setSystemProperty(HTTP_PROXY_HOST_KEY, httpProperties.getHost());
			setSystemProperty(HTTP_PROXY_PORT_KEY, httpProperties.getPort());
			setSystemProperty(HTTP_PROXY_USER_KEY, httpProperties.getUsername());
			setSystemProperty(HTTP_PROXY_PASS_KEY, httpProperties.getPassword());
		}
		if (httpsProperties != null) {
			setSystemProperty(HTTPS_PROXY_HOST_KEY, httpsProperties.getHost());
			setSystemProperty(HTTPS_PROXY_PORT_KEY, httpsProperties.getPort());
			setSystemProperty(HTTPS_PROXY_USER_KEY, httpsProperties.getUsername());
			setSystemProperty(HTTPS_PROXY_PASS_KEY, httpsProperties.getPassword());
		}

		String nonProxyHosts = getNonProxyHosts(this.properties);
		setSystemProperty(HTTP_NON_PROXY_HOSTS_KEY, nonProxyHosts);
		logger.info("Setting System property '" + HTTP_NON_PROXY_HOSTS_KEY
				+ "': " + nonProxyHosts);

		ProxyAuthenticator proxyAuthenticator = getProxyAuthenticator(this.properties);
		Authenticator.setDefault(proxyAuthenticator);
		logger.info("Setting default Authenticator for Git proxy");
	}

	private void setSystemProperty(String key, String value) {
		logger.info("Setting System property '" + key + "': " + value);
		if (StringUtils.hasText(value)) {
			System.setProperty(key, value);
		}
		else {
			System.clearProperty(key);
		}
	}

	private String getNonProxyHosts(GitProxyProperties proxyProperties) {
		GitProxyProperties.Http httpProperties = proxyProperties.getHttp();
		GitProxyProperties.Https httpsProperties = proxyProperties.getHttps();
		String httpNonProxyHosts = null;
		String httpsNonProxyHosts = null;
		if (httpProperties != null) {
			httpNonProxyHosts = httpProperties.getNonProxyHosts();
		}
		if (httpsProperties != null) {
			httpsNonProxyHosts = httpsProperties.getNonProxyHosts();
		}
		// nonProxyHosts from http scope takes precedence over https
		return StringUtils.hasText(httpNonProxyHosts) ? httpNonProxyHosts
				: httpsNonProxyHosts;
	}

	private ProxyAuthenticator getProxyAuthenticator(GitProxyProperties proxyProperties) {
		GitProxyProperties.Http httpProperties = proxyProperties.getHttp();
		GitProxyProperties.Https httpsProperties = proxyProperties.getHttps();
		// username and password from http scope takes precedence over https
		if (httpProperties != null
				&& StringUtils.hasText(httpProperties.getUsername())
				&& StringUtils.hasText(httpProperties.getPassword())) {
			return new ProxyAuthenticator(httpProperties.getUsername(),
					httpProperties.getPassword());
		}
		else if (httpsProperties != null
				&& StringUtils.hasText(httpsProperties.getUsername())
				&& StringUtils.hasText(httpsProperties.getPassword())) {
			return new ProxyAuthenticator(httpsProperties.getUsername(),
					httpsProperties.getPassword());
		}
		return null;
	}

	public static class ProxyAuthenticator extends Authenticator {

		private final PasswordAuthentication passwordAuthentication;

		private ProxyAuthenticator(String username, String password) {
			this.passwordAuthentication = new PasswordAuthentication(username,
					password.toCharArray());
		}

		protected PasswordAuthentication getPasswordAuthentication() {
			return this.passwordAuthentication;
		}

	}

}
