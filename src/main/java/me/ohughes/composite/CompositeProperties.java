package me.ohughes.composite;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import me.ohughes.proxy.properties.BaseProxyProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Pattern;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Container for composite property structure
 */
@Data
@ConfigurationProperties("spring.cloud.config.server")
public class CompositeProperties {

	private List<DeclarativeCompositeProperties> composite = new ArrayList<>();

	@Data
	@Validated
	@EqualsAndHashCode(callSuper = true)
	@ToString(callSuper = true)
	public static class DeclarativeCompositeProperties extends GitRepoProperties {
		@Pattern(regexp = "vault|git")
		private String type;
		private String host;
		private String scheme;
		private int port;
		private String backend;
		private String profileSeparator;
		private String defaultKey;
		private BaseProxyProperties proxy;
		private Map<String, PatternMatchingRepoProperties> repos = new LinkedHashMap<>();
	}

	@Data
	public static class GitRepoProperties {
		private boolean cloneOnStart;
		private boolean forcePull;
		private String uri;
		private String username;
		private String password;
		private String[] searchPaths = new String[0];
		private String basedir;
	}

	@Data
	@EqualsAndHashCode(callSuper = true)
	@ToString(callSuper = true)
	public static class PatternMatchingRepoProperties extends GitRepoProperties {
		private String[] pattern = new String[0];
		private String name;
	}
}
