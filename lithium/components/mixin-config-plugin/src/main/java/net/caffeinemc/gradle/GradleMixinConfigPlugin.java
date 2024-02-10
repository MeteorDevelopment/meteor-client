package net.caffeinemc.gradle;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class GradleMixinConfigPlugin implements Plugin<Project> {

    static final Logger LOGGER = LogManager.getLogger("CaffeineMc-MixinConfig");

    @Override
    public void apply(Project project) {
        project.getTasks().register("createMixinConfig", CreateMixinConfigTask.class);
    }
}