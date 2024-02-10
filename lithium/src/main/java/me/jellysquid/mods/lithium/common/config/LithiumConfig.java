package me.jellysquid.mods.lithium.common.config;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import me.jellysquid.mods.lithium.common.compat.worldedit.WorldEditCompat;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.CustomValue.CvType;
import net.fabricmc.loader.api.metadata.ModMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Documentation of these options: https://github.com/jellysquid3/lithium-fabric/wiki/Configuration-File
 */
@SuppressWarnings("CanBeFinal")
public class LithiumConfig {
    private static final Logger LOGGER = LogManager.getLogger("LithiumConfig");

    private static final String JSON_KEY_LITHIUM_OPTIONS = "lithium:options";

    private final Map<String, Option> options = new HashMap<>();
    private final Set<Option> optionsWithDependencies = new ObjectLinkedOpenHashSet<>();

    private void applyLithiumCompat() {
        Option option = this.options.get("mixin.block.hopper.worldedit_compat");
        if (!option.isEnabled() && WorldEditCompat.WORLD_EDIT_PRESENT) {
            option.addModOverride(true, "lithium-fabric");
        }
    }

    private LithiumConfig() {
        // Defines the default rules which can be configured by the user or other mods.
        InputStream defaultPropertiesStream = LithiumConfig.class.getResourceAsStream("/assets/lithium/lithium-mixin-config-default.properties");
        if (defaultPropertiesStream == null) {
            throw new IllegalStateException("Lithium mixin config default properties could not be read!");
        }
        try (BufferedReader propertiesReader = new BufferedReader(new InputStreamReader(defaultPropertiesStream))) {
            Properties properties = new Properties();
            properties.load(propertiesReader);
            properties.forEach((ruleName, enabled) -> this.addMixinRule((String) ruleName, Boolean.parseBoolean((String) enabled)));
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Lithium mixin config default properties could not be read!");
        }

        InputStream dependenciesStream = LithiumConfig.class.getResourceAsStream("/assets/lithium/lithium-mixin-config-dependencies.properties");
        if (dependenciesStream == null) {
            throw new IllegalStateException("Lithium mixin config dependencies could not be read!");
        }
        try (BufferedReader propertiesReader = new BufferedReader(new InputStreamReader(dependenciesStream))) {
            Properties properties = new Properties();
            properties.load(propertiesReader);
            properties.forEach(
                    (o1, o2) -> {
                        String rulename = (String) o1;
                        String dependencies = (String) o2;
                        String[] dependenciesSplit = dependencies.split(",");
                        for (String dependency : dependenciesSplit) {
                            String[] split = dependency.split(":");
                            if (split.length != 2) {
                                return;
                            }
                            String dependencyName = split[0];
                            String requiredState = split[1];
                            this.addRuleDependency(rulename, dependencyName, Boolean.parseBoolean(requiredState));
                        }
                    }
            );
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Lithium mixin config dependencies could not be read!");
        }
    }

    /**
     * Loads the configuration file from the specified location. If it does not exist, a new configuration file will be
     * created. The file on disk will then be updated to include any new options.
     */
    public static LithiumConfig load(File file) {
        LithiumConfig config = new LithiumConfig();

        if (file.exists()) {
            Properties props = new Properties();

            try (FileInputStream fin = new FileInputStream(file)) {
                props.load(fin);
            } catch (IOException e) {
                throw new RuntimeException("Could not load config file", e);
            }

            config.readProperties(props);
        } else {
            try {
                writeDefaultConfig(file);
            } catch (IOException e) {
                LOGGER.warn("Could not write default configuration file", e);
            }
        }
        config.applyLithiumCompat();

        config.applyModOverrides();

        // Check dependencies several times, because one iteration may disable a rule required by another rule
        // This terminates because each additional iteration will disable one or more rules, and there is only a finite number of rules
        //noinspection StatementWithEmptyBody
        while (config.applyDependencies()) {
            //noinspection UnnecessarySemicolon
            ;
        }

        return config;
    }

    /**
     * Defines a dependency between two registered mixin rules. If a dependency is not satisfied, the mixin will
     * be disabled.
     *
     * @param rule          the mixin rule that requires another rule to be set to a given value
     * @param dependency    the mixin rule the given rule depends on
     * @param requiredValue the required value of the dependency
     */
    @SuppressWarnings("SameParameterValue")
    private void addRuleDependency(String rule, String dependency, boolean requiredValue) {
        Option option = this.options.get(rule);
        if (option == null) {
            LOGGER.error("Option {} for dependency '{} depends on {}={}' not found. Skipping.", rule, rule, dependency, requiredValue);
            return;
        }
        Option dependencyOption = this.options.get(dependency);
        if (dependencyOption == null) {
            LOGGER.error("Option {} for dependency '{} depends on {}={}' not found. Skipping.", dependency, rule, dependency, requiredValue);
            return;
        }
        option.addDependency(dependencyOption, requiredValue);
        this.optionsWithDependencies.add(option);
    }


    /**
     * Defines a Mixin rule which can be configured by users and other mods.
     *
     * @param mixin   The name of the mixin package which will be controlled by this rule
     * @param enabled True if the rule will be enabled by default, otherwise false
     * @throws IllegalStateException If a rule with that name already exists
     */
    private void addMixinRule(String mixin, boolean enabled) {
        if (this.options.putIfAbsent(mixin, new Option(mixin, enabled, false)) != null) {
            throw new IllegalStateException("Mixin rule already defined: " + mixin);
        }
    }

    private void readProperties(Properties props) {
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();

            Option option = this.options.get(key);

            if (option == null) {
                LOGGER.warn("No configuration key exists with name '{}', ignoring", key);
                continue;
            }

            boolean enabled;

            if (value.equalsIgnoreCase("true")) {
                enabled = true;
            } else if (value.equalsIgnoreCase("false")) {
                enabled = false;
            } else {
                LOGGER.warn("Invalid value '{}' encountered for configuration key '{}', ignoring", value, key);
                continue;
            }

            option.setEnabled(enabled, true);
        }
    }

    private void applyModOverrides() {
        for (ModContainer container : FabricLoader.getInstance().getAllMods()) {
            ModMetadata meta = container.getMetadata();

            if (meta.containsCustomValue(JSON_KEY_LITHIUM_OPTIONS)) {
                CustomValue overrides = meta.getCustomValue(JSON_KEY_LITHIUM_OPTIONS);

                if (overrides.getType() != CvType.OBJECT) {
                    LOGGER.warn("Mod '{}' contains invalid Lithium option overrides, ignoring", meta.getId());
                    continue;
                }

                for (Map.Entry<String, CustomValue> entry : overrides.getAsObject()) {
                    this.applyModOverride(meta, entry.getKey(), entry.getValue());
                }
            }
        }
    }

    private void applyModOverride(ModMetadata meta, String name, CustomValue value) {
        if (!name.startsWith("mixin.")) {
            name = getMixinRuleName(name);
        }
        Option option = this.options.get(name);

        if (option == null) {
            LOGGER.warn("Mod '{}' attempted to override option '{}', which doesn't exist, ignoring", meta.getId(), name);
            return;
        }

        if (value.getType() != CvType.BOOLEAN) {
            LOGGER.warn("Mod '{}' attempted to override option '{}' with an invalid value, ignoring", meta.getId(), name);
            return;
        }

        boolean enabled = value.getAsBoolean();

        // disabling the option takes precedence over enabling
        if (!enabled && option.isEnabled()) {
            option.clearModsDefiningValue();
        }

        if (!enabled || option.isEnabled() || option.getDefiningMods().isEmpty()) {
            option.addModOverride(enabled, meta.getId());
        }
    }

    /**
     * Returns the effective option for the specified class name. This traverses the package path of the given mixin
     * and checks each root for configuration rules. If a configuration rule disables a package, all mixins located in
     * that package and its children will be disabled. The effective option is that of the highest-priority rule, either
     * a enable rule at the end of the chain or a disable rule at the earliest point in the chain.
     *
     * @return Null if no options matched the given mixin name, otherwise the effective option for this Mixin
     */
    public Option getEffectiveOptionForMixin(String mixinClassName) {
        int lastSplit = 0;
        int nextSplit;

        Option rule = null;

        while ((nextSplit = mixinClassName.indexOf('.', lastSplit)) != -1) {
            String key = getMixinRuleName(mixinClassName.substring(0, nextSplit));

            Option candidate = this.options.get(key);

            if (candidate != null) {
                rule = candidate;

                if (!rule.isEnabled()) {
                    return rule;
                }
            }

            lastSplit = nextSplit + 1;
        }

        return rule;
    }

    /**
     * Tests all dependencies and disables options when their dependencies are not met.
     */
    private boolean applyDependencies() {
        boolean changed = false;
        for (Option optionWithDependency : this.optionsWithDependencies) {
            changed |= optionWithDependency.disableIfDependenciesNotMet(LOGGER, this);
        }
        return changed;
    }

    private static void writeDefaultConfig(File file) throws IOException {
        File dir = file.getParentFile();

        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException("Could not create parent directories");
            }
        } else if (!dir.isDirectory()) {
            throw new IOException("The parent file is not a directory");
        }

        try (Writer writer = new FileWriter(file)) {
            writer.write("# This is the configuration file for Lithium.\n");
            writer.write("# This file exists for debugging purposes and should not be configured otherwise.\n");
            writer.write("# Before configuring anything, take a backup of the worlds that will be opened.\n");
            writer.write("#\n");
            writer.write("# You can find information on editing this file and all the available options here:\n");
            writer.write("# https://github.com/jellysquid3/lithium-fabric/wiki/Configuration-File\n");
            writer.write("#\n");
            writer.write("# By default, this file will be empty except for this notice.\n");
        }
    }

    private static String getMixinRuleName(String name) {
        return "mixin." + name;
    }

    public int getOptionCount() {
        return this.options.size();
    }

    public int getOptionOverrideCount() {
        return (int) this.options.values()
                .stream()
                .filter(Option::isOverridden)
                .count();
    }

    public Option getParent(Option option) {
        String optionName = option.getName();
        int split;

        if ((split = optionName.lastIndexOf('.')) != -1) {
            String key = optionName.substring(0, split);
            return this.options.get(key);

        }
        return null;
    }
}
