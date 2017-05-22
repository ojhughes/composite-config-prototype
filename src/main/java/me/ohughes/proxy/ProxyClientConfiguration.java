package me.ohughes.proxy;

import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class ProxyClientConfiguration {

	public static RestTemplateBuilder getRestTemplateBuilder(ObjectProvider<HttpMessageConverters> messageConverters, ObjectProvider<List<RestTemplateCustomizer>> restTemplateCustomizers) {
		RestTemplateBuilder builder = new RestTemplateBuilder();
		HttpMessageConverters converters = messageConverters.getIfUnique();
		if (converters != null) {
			builder = builder.messageConverters(converters.getConverters());
		}

		ArrayList<RestTemplateCustomizer> customizers = new ArrayList<>();
		List<RestTemplateCustomizer> availableCustomizers = restTemplateCustomizers.getIfAvailable();
		if (availableCustomizers != null) {
			customizers.addAll(availableCustomizers);
		}
		if (!CollectionUtils.isEmpty(customizers)) {
			AnnotationAwareOrderComparator.sort(customizers);
			builder = builder.customizers(customizers);
		}
		return builder;
	}

	@Bean
    HttpClientBuilder httpClientBuilder() {
		return HttpClientBuilder.create();
	}

}
