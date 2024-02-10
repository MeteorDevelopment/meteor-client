package net.caffeinemc.gradle;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class DefaultConfigCreator {
    public static void writeDefaultConfig(String projectName, File file, List<CreateMixinConfigTask.MixinRuleRepresentation> sortedMixinConfigOptions) throws IOException {
        try (Writer writer = new FileWriter(file)) {
            writer.write("# This is the default config file for " + projectName + ".\n");
            writer.write("# This file should not be modified manually. Edit the .properties file in the config folder in the .minecraft folder instead!\n");
            writer.write("#\n");
            for (CreateMixinConfigTask.MixinRuleRepresentation option : sortedMixinConfigOptions) {
                writer.write(option.path() + "=" + option.config().enabled() + "\n");
            }
        }
    }

    public static void writeMixinDependencies(String projectName, File file, List<CreateMixinConfigTask.MixinRuleRepresentation> sortedMixinConfigOptions) throws IOException {
        try (Writer writer = new FileWriter(file)) {
            writer.write("# This is the config dependency file for " + projectName + ".\n");
            writer.write("# This file should not be modified!\n");
            writer.write("#\n");
            for (CreateMixinConfigTask.MixinRuleRepresentation option : sortedMixinConfigOptions) {
                if (option.config().depends().length > 0) {
                    MixinConfigDependency[] dependencies = option.config().depends();
                    StringBuilder dependencyList = new StringBuilder();
                    for (int i = 0; i < dependencies.length; i++) {
                        MixinConfigDependency dependency = dependencies[i];
                        dependencyList.append(dependency.dependencyPath()).append(":").append(dependency.enabled());
                        if (i != dependencies.length - 1) {
                            dependencyList.append(",");
                        }
                    }
                    writer.write(option.path() + "=" + dependencyList + "\n");
                }
            }
        }
    }

    public static void writeMixinConfigSummaryMarkdown(String projectName, File file, List<CreateMixinConfigTask.MixinRuleRepresentation> sortedMixinConfigOptions) throws IOException {
        try (Writer writer = new FileWriter(file)) {
            writer.write("# " + projectName + " Configuration File Summary\n");
            writer.write(
                    """
                            The configuration file makes use of the [Java properties format](https://docs.oracle.com/cd/E23095_01/Platform.93/ATGProgGuide/html/s0204propertiesfileformat01.html). If the configuration file does not exist during game start-up, a blank file with a comment will be created.

                            The configuration file defines *overrides* for the available options, and as such, a blank file is perfectly normal! It simply means that you'd like to use all the default values.

                            Each category below includes a list of options which can be changed by the user. Due to the nature of the mod, configuration options require a game restart to take effect.

                            ### Editing the configuration file

                            Before editing the configuration file, take a backup of your minecraft worlds!
                            All configuration options are simple key-value pairs. In other words, you first specify the option's name, followed by the desired value, like so:

                            ```properties
                            mixin.ai.pathing=false
                            mixin.gen.biome_noise_cache=false
                            ```

                            # Configuration options
                            """);
            for (CreateMixinConfigTask.MixinRuleRepresentation option : sortedMixinConfigOptions) {
                writer.write("### `" + option.path() + "`\n");
                writer.write("(default: `" + option.config().enabled() + "`)  \n");
                if (option.config().description().length() > 0) {
                    writer.write(option.config().description() + "  \n");
                }
                if (option.config().depends().length > 0) {
                    MixinConfigDependency[] dependencies = option.config().depends();
                    StringBuilder dependencyList = new StringBuilder();
                    for (int i = 0; i < dependencies.length; i++) {
                        MixinConfigDependency dependency = dependencies[i];
                        dependencyList.append("- `").append(dependency.dependencyPath()).append("=").append(dependency.enabled()).append("`");
                        if (i != dependencies.length - 1) {
                            dependencyList.append("\n");
                        }
                    }
                    writer.write("Requirements:\n" + dependencyList + "  \n");
                }
                writer.write("  \n");
            }
        }
    }
}
