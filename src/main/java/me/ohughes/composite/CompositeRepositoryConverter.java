package me.ohughes.composite;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.cloud.config.server.environment.MultipleJGitEnvironmentRepository;
import org.springframework.cloud.config.server.environment.VaultEnvironmentRepository;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

import java.beans.FeatureDescriptor;
import java.io.File;
import java.util.*;
import java.util.stream.Stream;

/**
 * Convert POJOs derived from composite @ConfigurationProperties into appropriate (Git or Vault) EnvironmentRepository
 */
@Slf4j
@Component
public class CompositeRepositoryConverter {


	MultipleJGitEnvironmentRepository convertPropertiesToGitEnvironment(ConfigurableEnvironment environment, CompositeProperties.DeclarativeCompositeProperties configProperties) {
		MultipleJGitEnvironmentRepository multiGitEnv = new MultipleJGitEnvironmentRepository(environment);
		Map<String, MultipleJGitEnvironmentRepository.PatternMatchingJGitEnvironmentRepository> convertedRepoMap = new LinkedHashMap<>();
		BeanUtils.copyProperties(configProperties, multiGitEnv, "repos");

		// Important to explicitly set basedir as property types do not match
		setGitBasedirIfAvailable(configProperties, multiGitEnv);

		//Guard against null value for repos map
		Set<Map.Entry<String, CompositeProperties.PatternMatchingRepoProperties>> extraRepoProperties = Optional
			.ofNullable(configProperties.getRepos())
			.map(Map::entrySet)
			.orElse(Collections.emptySet());

		//Map properties for each set of nested git repositories individually
		for (Map.Entry<String, CompositeProperties.PatternMatchingRepoProperties> patternRepoEntry : extraRepoProperties){
			MultipleJGitEnvironmentRepository.PatternMatchingJGitEnvironmentRepository patternMatchingRepo = new MultipleJGitEnvironmentRepository.PatternMatchingJGitEnvironmentRepository();

			// Important that the patternMatchingrepo has its environment set as well as its name. Failure to set
			// the environment will cause property searches to fail.
			patternMatchingRepo.setName(patternRepoEntry.getKey());
			patternMatchingRepo.setEnvironment(multiGitEnv.getEnvironment());

			BeanUtils.copyProperties(patternRepoEntry.getValue(), patternMatchingRepo, getNullPropertyNames(patternRepoEntry.getValue()));

			// Important to explicitly set basedir as property types do not match
			setGitBasedirIfAvailable(patternRepoEntry, patternMatchingRepo);

			convertedRepoMap.put(patternMatchingRepo.getName(), patternMatchingRepo);
		}

		multiGitEnv.setRepos(convertedRepoMap);
		requestGitCloneOnStart(multiGitEnv);

		return multiGitEnv;
	}

	public VaultEnvironmentRepository convertPropertiesToVaultEnvironment(Object configProperties, VaultEnvironmentRepository vaultEnv) {

		BeanUtils.copyProperties(configProperties, vaultEnv, getNullPropertyNames(configProperties));
		return vaultEnv;
	}

	private void setGitBasedirIfAvailable(CompositeProperties.DeclarativeCompositeProperties configProperties,
										  MultipleJGitEnvironmentRepository multiGitEnv) {
		if (null != configProperties.getBasedir()) {
			multiGitEnv.setBasedir(new File(configProperties.getBasedir()));
		}
	}

	private void setGitBasedirIfAvailable(Map.Entry<String, CompositeProperties.PatternMatchingRepoProperties> patternRepoEntry,
									   MultipleJGitEnvironmentRepository.PatternMatchingJGitEnvironmentRepository patternMatchingRepo) {
		if (null != patternRepoEntry.getValue().getBasedir()) {
			patternMatchingRepo.setBasedir(new File(patternRepoEntry.getValue().getBasedir()));
		}
	}

	/**
	 * Requests the supplied {@code multipleJGitEnvironmentRepository} to carry out a "start up" clone of its git repositories to
	 * local storage by invoking its {@link MultipleJGitEnvironmentRepository#afterPropertiesSet()} method.<br/>
	 * Note 1: that this call will also trigger a git clone action on any sub-repositories that are contained by
	 * {@code multipleJGitEnvironmentRepository}.
	 * Note 2: any "cloneOnStart" value for this repository (and its sub-repositories) will be respected when
	 * determining whether or not to carry out the clone action. Only those repositories that have a "cloneOnStart"
	 * property with a value of {@code true} will be cloned. The remaining repositories will exhibit "clone on demand"
	 * behaviour and only clone from their remote git repository when a subsequent configuration request demands it.
	 *
	 * @param multipleJGitEnvironmentRepository a git environment repository
	 */
	private void requestGitCloneOnStart(MultipleJGitEnvironmentRepository multipleJGitEnvironmentRepository) {
		try {
			multipleJGitEnvironmentRepository.afterPropertiesSet();
		} catch (Exception e) {
			log.info("Error occurred attempting to clone git repo on start: {} ", e.getMessage());
			throw new IllegalStateException(String.format("Error occurred attempting to clone git repo on start: %s",
				e.getMessage()), e);
		}
	}

	private static String[] getNullPropertyNames (Object source) {
		final BeanWrapper wrappedSource = new BeanWrapperImpl(source);
		return Stream.of(wrappedSource.getPropertyDescriptors())
			.map(FeatureDescriptor::getName)
			.filter(propertyName -> wrappedSource.getPropertyValue(propertyName) == null)
			.toArray(String[]::new);
	}
}
