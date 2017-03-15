package me.ohughes.environment;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.server.environment.AbstractScmEnvironmentRepository;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.cloud.config.server.environment.JGitEnvironmentRepository;
import org.springframework.cloud.config.server.environment.VaultEnvironmentRepository;

import java.util.List;

/**
 * Created by ohughes on 3/15/17.
 */
@Data
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class HeteroCompositeEnvironmentRepository implements EnvironmentRepository {

    private List<VaultEnvironmentRepository> vaultEnvironments;
    private List<AbstractScmEnvironmentRepository> gitEnvironments;

    @Override
    public Environment findOne(String application, String profile, String label) {
        return null;
    }
}
