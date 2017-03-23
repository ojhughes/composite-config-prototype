package me.ohughes.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spring.cloud.config.server.git.proxy")
public class GitProxyProperties {

	private Https https;

	private Http http;

	public Https getHttps() {
		return https;
	}

	public void setHttps(Https https) {
		this.https = https;
	}

	public Http getHttp() {
		return http;
	}

	public void setHttp(Http http) {
		this.http = http;
	}

	public static class Https extends AbstractHttp {

	}

	public static class Http extends AbstractHttp {

	}

	public abstract static class AbstractHttp {

		private String host;

		private String port;

		private String nonProxyHosts;

		private String username;

		private String password;

		public String getHost() {
			return host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public String getPort() {
			return port;
		}

		public void setPort(String port) {
			this.port = port;
		}

		public String getNonProxyHosts() {
			return nonProxyHosts;
		}

		public void setNonProxyHosts(String nonProxyHosts) {
			this.nonProxyHosts = nonProxyHosts;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

	}

}
