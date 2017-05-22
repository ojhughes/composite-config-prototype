package me.ohughes.composite;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.cloud.config.server.config.ConfigServerAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Created by ohughes on 5/17/17.
 */
@AutoConfigureBefore(ConfigServerAutoConfiguration.class)
@Import(DeclarativeCompositeConfig.class)
public class CompositeAutoConfiguration {
}
