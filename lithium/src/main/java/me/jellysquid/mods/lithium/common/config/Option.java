package me.jellysquid.mods.lithium.common.config;

import it.unimi.dsi.fastutil.objects.Object2BooleanLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class Option {
    private final String name;

    private Object2BooleanLinkedOpenHashMap<Option> dependencies;
    private Set<String> modDefined = null;
    private boolean enabled;
    private boolean userDefined;

    public Option(String name, boolean enabled, boolean userDefined) {
        this.name = name;
        this.enabled = enabled;
        this.userDefined = userDefined;
    }

    public void setEnabled(boolean enabled, boolean userDefined) {
        this.enabled = enabled;
        this.userDefined = userDefined;
    }

    public void addModOverride(boolean enabled, String modId) {
        this.enabled = enabled;

        if (this.modDefined == null) {
            this.modDefined = new LinkedHashSet<>();
        }

        this.modDefined.add(modId);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public boolean isEnabledRecursive(LithiumConfig config) {
        return this.enabled && (config.getParent(this) == null || config.getParent(this).isEnabledRecursive(config));
    }

    public boolean isOverridden() {
        return this.isUserDefined() || this.isModDefined();
    }

    public boolean isUserDefined() {
        return this.userDefined;
    }

    public boolean isModDefined() {
        return this.modDefined != null;
    }

    public String getName() {
        return this.name;
    }

    public void clearModsDefiningValue() {
        this.modDefined = null;
    }

    public Collection<String> getDefiningMods() {
        return this.modDefined != null ? Collections.unmodifiableCollection(this.modDefined) : Collections.emptyList();
    }

    public void addDependency(Option dependencyOption, boolean requiredValue) {
        if (this.dependencies == null) {
            this.dependencies = new Object2BooleanLinkedOpenHashMap<>(1);
        }
        this.dependencies.put(dependencyOption, requiredValue);
    }

    public boolean disableIfDependenciesNotMet(Logger logger, LithiumConfig config) {
        if (this.dependencies != null && this.isEnabled()) {
            for (Object2BooleanMap.Entry<Option> dependency : this.dependencies.object2BooleanEntrySet()) {
                Option option = dependency.getKey();
                boolean requiredValue = dependency.getBooleanValue();
                if (option.isEnabledRecursive(config) != requiredValue) {
                    this.enabled = false;
                    logger.warn("Option '{}' requires '{}={}' but found '{}'. Setting '{}={}'.", this.name, option.name, requiredValue, option.isEnabled(), this.name, this.enabled);
                    return true;
                }
            }
        }
        return false;
    }
}
