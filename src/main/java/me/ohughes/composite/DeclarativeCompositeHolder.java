package me.ohughes.composite;

import lombok.Data;
import org.apache.http.client.HttpClient;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;

/**
 * Data class to hold an environment repository along with it's HttpClient.
 * This is used by the {@link DeclarativeCompositeEnvironmentRepository}
 * so that {@link org.apache.http.client.HttpClient} configuration can be customised differently for each item within the composite.
 */

@Data
class DeclarativeCompositeHolder {
	private EnvironmentRepository environmentRepository;
	private HttpClient httpClient;
}
