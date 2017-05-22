package me.ohughes.composite;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import me.ohughes.proxy.ProxyAwareHttpClientFactory;
import org.apache.http.client.HttpClient;
import org.eclipse.jgit.transport.HttpTransport;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.core.Ordered;

import java.util.*;
import java.util.Map.Entry;

/**
 * Master environment repository that searches for a config value
 * using all repositories in a composite list in order
 * <p>
 * Currently the implementation of findOne() does not account for individual environment repositories being refreshed
 * during, or after, the time where the composite environment is being constructed / deduplicated. This could potentially lead to
 * a scenario where a composite collection contains a stale property. In SCS, properties are populated when an application
 * is bound to a config server service instance or when a refresh is manually triggered.
 * See research task: https://www.pivotaltracker.com/story/show/143023479
 */
@Slf4j
@Data
@AllArgsConstructor
public class DeclarativeCompositeEnvironmentRepository implements EnvironmentRepository, Ordered {

	private List<DeclarativeCompositeHolder> environmentRepositories;

	@Override
	public Environment findOne(String application, String profile, String label) {
		Environment compositeEnvironment = new Environment(application, new String[]{profile}, label, null, null);

		try {
			Set<Object> foundKeys = new HashSet<>();
			for (DeclarativeCompositeHolder environmentHolder : environmentRepositories) {
				EnvironmentRepository environmentRepository = environmentHolder.getEnvironmentRepository();
//				if (environmentRepository instanceof JGitEnvironmentRepository) {
//					setupGitProxy(environmentHolder.getHttpClient());
//				}
				Environment env = environmentRepository.findOne(application, profile, label);
				ProxyAwareHttpClientFactory jgitTransport = (ProxyAwareHttpClientFactory) HttpTransport.getConnectionFactory();
				jgitTransport.getHttpClientConnection().close();
				//Each environment repository has a set of key/value properties. Deduplicate the property values
				//so only the first found is returned to the client.
				for (PropertySource source : env.getPropertySources()) {
					Map<Object, Object> newSourceMap = deduplicatePropertySource(foundKeys, source);

					// Only add a new PropertySource to the returned environment if the de-duped source map contains
					// at least one item
					if (null != newSourceMap && newSourceMap.size() > 0) {
						compositeEnvironment.add(new PropertySource(source.getName(), newSourceMap));
					}
				}
			}
		} catch (Exception e) {
			log.error(String.format("Error while searching composite back ends: %s", e.getMessage()), e);
			throw new RuntimeException(e);
		}

		return compositeEnvironment;
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 1;
	}

	Map<Object, Object> deduplicatePropertySource(Set<Object> foundKeys, PropertySource source) {
		//Guard against null value for repos map
		Set<? extends Entry<?, ?>> sourceValueCollection = Optional
			.ofNullable(source.getSource())
			.map(Map::entrySet)
			.orElse(Collections.emptySet());

		Map<Object, Object> newSourceMap = new HashMap<>();
		for (Entry<?, ?> sourceValueEntry : sourceValueCollection) {
			if (!foundKeys.contains(sourceValueEntry.getKey())) {
				newSourceMap.put(sourceValueEntry.getKey(), sourceValueEntry.getValue());
				foundKeys.add(sourceValueEntry.getKey());
			}
		}
		return newSourceMap;
	}

	private void setupGitProxy(HttpClient httpClient) {
		HttpTransport.setConnectionFactory(new ProxyAwareHttpClientFactory(httpClient));
	}
}
