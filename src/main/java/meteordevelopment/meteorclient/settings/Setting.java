/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import meteordevelopment.meteorclient.addons.AddonManager;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.translation.TranslationKey;
import meteordevelopment.meteorclient.translation.TranslationManager;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.IGetter;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class Setting<T> implements IGetter<T>, ISerializable<T> {
    private static final List<String> NO_SUGGESTIONS = List.of();

    public final String name, description;
    private final IVisible visible;

    private TranslationKey nameKey;
    private TranslationKey descriptionKey;
    private String namespaceScope;
    private String ownerScope;
    private String groupScope = "General";
    private String cachedKeyOwner;

    protected final T defaultValue;
    protected T value;

    public final Consumer<Setting<T>> onModuleActivated;
    private final Consumer<T> onChanged;

    public Module module;
    public boolean lastWasVisible;

    public Setting(String name, String description, T defaultValue, Consumer<T> onChanged, Consumer<Setting<T>> onModuleActivated, IVisible visible) {
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.onChanged = onChanged;
        this.onModuleActivated = onModuleActivated;
        this.visible = visible;

        resetImpl();
    }

    /**
     * Assign the translation namespace (mod id) and owner id for this setting. Called by
     * {@link Settings#assignOwner(String, String)} when the owning module or HUD element is
     * created, and by {@link Settings#registerColorSettings(Module)} for module settings.
     * Until assigned, keys are scoped to the meteor-client global namespace.
     */
    public void assignOwner(String namespace, String ownerId) {
        this.namespaceScope = namespace;
        this.ownerScope = ownerId;
        this.module = null;
    }

    /** Assign the owner id, keeping the meteor-client namespace. */
    public void assignOwner(String ownerId) {
        assignOwner(TranslationManager.METEOR_MOD_ID, ownerId);
    }

    /** Assign the setting-group id used to scope translation keys. */
    public void assignGroup(String groupId) {
        this.groupScope = groupId;
    }

    /** The translation namespace (mod id) used to scope translation keys. */
    private String namespaceId() {
        if (namespaceScope != null) return namespaceScope;
        if (module != null) return AddonManager.namespaceOf(module.getClass());
        String ambient = AddonManager.initializingNamespace();
        return ambient != null ? ambient : TranslationManager.METEOR_MOD_ID;
    }

    /** The owner id used to scope translation keys — explicit scope, module name, or "global". */
    private String ownerId() {
        if (ownerScope != null) return ownerScope;
        return module != null ? module.name : TranslationManager.GLOBAL_SCOPE;
    }

    private void ensureKeys() {
        String cacheId = namespaceId() + "/" + ownerId() + "/" + groupScope;
        if (nameKey == null || !cacheId.equals(cachedKeyOwner)) {
            cachedKeyOwner = cacheId;
            nameKey = TranslationKey.of(TranslationManager.settingNameKey(namespaceId(), ownerId(), groupScope, name));
            descriptionKey = TranslationKey.of(TranslationManager.settingDescriptionKey(namespaceId(), ownerId(), groupScope, name));
        }
    }

    /** Display title in the current language, resolved lazily on every call. */
    public String title() {
        ensureKeys();
        return nameKey.get();
    }

    /** Description in the current language, resolved lazily on every call. */
    public String description() {
        ensureKeys();
        return descriptionKey.get();
    }

    /** The raw translation key backing {@link #title()}. */
    public TranslationKey nameKey() {
        ensureKeys();
        return nameKey;
    }

    /** The raw translation key backing {@link #description()}. */
    public TranslationKey descriptionKey() {
        ensureKeys();
        return descriptionKey;
    }

    @Override
    public T get() {
        return value;
    }

    public boolean set(T value) {
        if (!isValueValid(value)) return false;
        this.value = value;
        onChanged();
        return true;
    }

    protected void resetImpl() {
        value = defaultValue;
    }

    public void reset() {
        resetImpl();
        onChanged();
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public boolean parse(String str) {
        T newValue = parseImpl(str);

        if (newValue != null) {
            if (isValueValid(newValue)) {
                value = newValue;
                onChanged();
            }
        }

        return newValue != null;
    }

    public boolean wasChanged() {
        return !Objects.equals(value, defaultValue);
    }

    public void onChanged() {
        if (onChanged != null) onChanged.accept(value);
    }

    public void onActivated() {
        if (onModuleActivated != null) onModuleActivated.accept(this);
    }

    public boolean isVisible() {
        return visible == null || visible.isVisible();
    }

    protected abstract T parseImpl(String str);

    protected abstract boolean isValueValid(T value);

    public Iterable<Identifier> getIdentifierSuggestions() {
        return null;
    }

    public Iterable<String> getSuggestions() {
        return NO_SUGGESTIONS;
    }

    protected abstract CompoundTag save(CompoundTag tag);

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.putString("name", name);
        save(tag);

        return tag;
    }

    protected abstract T load(CompoundTag tag);

    @Override
    public T fromTag(CompoundTag tag) {
        T value = load(tag);
        onChanged();

        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Setting<?> setting = (Setting<?>) o;
        return Objects.equals(name, setting.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Nullable
    public static <T> T parseId(Registry<T> registry, String name) {
        name = name.trim();

        Identifier id;
        if (name.contains(":")) id = Identifier.parse(name);
        else id = Identifier.withDefaultNamespace(name);
        if (registry.containsKey(id)) return registry.getValue(id);

        return null;
    }

    @SuppressWarnings("unchecked")
    public abstract static class SettingBuilder<B, V, S> {
        protected String name = "undefined", description = "";
        protected V defaultValue;
        protected IVisible visible;
        protected Consumer<V> onChanged;
        protected Consumer<Setting<V>> onModuleActivated;

        protected SettingBuilder(V defaultValue) {
            this.defaultValue = defaultValue;
        }

        public B name(String name) {
            this.name = name;
            return (B) this;
        }

        public B description(String description) {
            this.description = description;
            return (B) this;
        }

        public B defaultValue(V defaultValue) {
            this.defaultValue = defaultValue;
            return (B) this;
        }

        public B visible(IVisible visible) {
            this.visible = visible;
            return (B) this;
        }

        public B onChanged(Consumer<V> onChanged) {
            this.onChanged = onChanged;
            return (B) this;
        }

        public B onModuleActivated(Consumer<Setting<V>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return (B) this;
        }

        public abstract S build();
    }
}
