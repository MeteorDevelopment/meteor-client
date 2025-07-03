/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.meteor.ActiveModulesChangedEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.ModuleBindChangedEvent;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.world.Excavator;
import meteordevelopment.meteorclient.systems.modules.world.InfinityMiner;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.ValueComparableMap;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.lwjgl.glfw.GLFW;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Modules extends System<Modules> {
    private static final List<Category> CATEGORIES = new ArrayList<>();
    private static final Set<String> SCAN_PACKAGES = new HashSet<>();
    private static final Map<String, Supplier<Boolean>> CONDITIONAL_MODULES = new HashMap<>();

    static {
        registerModulePackage("meteordevelopment.meteorclient.systems.modules");

        registerConditionalModule(Excavator.class, () -> BaritoneUtils.IS_AVAILABLE);
        registerConditionalModule(InfinityMiner.class, () -> BaritoneUtils.IS_AVAILABLE);
    }

    private final Map<Class<? extends Module>, Module> moduleInstances = new Reference2ReferenceOpenHashMap<>();
    private final Map<Category, List<Module>> groups = new Reference2ReferenceOpenHashMap<>();

    private final List<Module> active = new ArrayList<>();
    private Module moduleToBind;
    private boolean awaitingKeyRelease = false;

    public Modules() {
        super("modules");
    }

    public static Modules get() {
        return Systems.get(Modules.class);
    }

    @Override
    public void init() {
        loadModules();
    }

    @Override
    public void load(File folder) {
        for (Module module : getAll()) {
            for (SettingGroup group : module.settings) {
                for (Setting<?> setting : group) setting.reset();
            }
        }

        super.load(folder);
    }

    /**
     * Registers a package to scan for modules.
     * This should be called by addons during their initialization.
     */
    public static void registerModulePackage(String packageName) {
        SCAN_PACKAGES.add(packageName);
    }

    /**
     * Registers a module to be loaded conditionally.
     *
     * @param moduleClass The class of the module to register
     * @param condition   A supplier that returns true if the module should be loaded
     */
    public static void registerConditionalModule(Class<? extends Module> moduleClass, Supplier<Boolean> condition) {
        CONDITIONAL_MODULES.put(moduleClass.getName(), condition);
    }

    public void loadModules() {
        try {
            long startTime = java.lang.System.currentTimeMillis();

            ConfigurationBuilder config = new ConfigurationBuilder()
                .forPackages(SCAN_PACKAGES.toArray(new String[0]))
                .setScanners(Scanners.SubTypes)
                .setParallel(true)
                .setExpandSuperTypes(false);

            Reflections reflections = new Reflections(config);
            Set<Class<? extends Module>> moduleClasses = reflections.getSubTypesOf(Module.class);

            moduleClasses = moduleClasses.stream()
                .filter(moduleClass -> SCAN_PACKAGES.stream().anyMatch(pkg -> moduleClass.getName().startsWith(pkg)))
                .collect(Collectors.toSet());

            Map<String, List<Class<? extends Module>>> modulesByPackage = new HashMap<>();
            for (Class<? extends Module> moduleClass : moduleClasses) {
                String packageName = moduleClass.getPackage().getName();
                modulesByPackage.computeIfAbsent(packageName, k -> new ArrayList<>()).add(moduleClass);
            }

            int totalCount = 0;
            int skippedCount = 0;
            for (Class<? extends Module> moduleClass : moduleClasses) {
                String className = moduleClass.getName();
                if (CONDITIONAL_MODULES.containsKey(className) && !CONDITIONAL_MODULES.get(className).get()) {
                    MeteorClient.LOG.info("Skipping conditional module: {}", className);
                    skippedCount++;
                    continue;
                }

                try {
                    if (Modifier.isAbstract(moduleClass.getModifiers()) || moduleClass.isInterface()) continue;

                    Constructor<? extends Module> constructor = moduleClass.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    Module module = constructor.newInstance();
                    add(module);
                    totalCount++;
                } catch (NoSuchMethodException ignored) {
                    MeteorClient.LOG.error("Module {} does not have a no-args constructor", moduleClass.getName());
                } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                    MeteorClient.LOG.error("Failed to load module: {}", moduleClass.getName(), e);
                }
            }

            long endTime = java.lang.System.currentTimeMillis();

            for (Map.Entry<String, List<Class<? extends Module>>> entry : modulesByPackage.entrySet()) {
                String packageName = entry.getKey();
                int count = entry.getValue().size();
                MeteorClient.LOG.info("Found {} modules in {}", count, packageName);
            }

            MeteorClient.LOG.info("Loaded {} modules ({} skipped) in {}ms", totalCount, skippedCount, endTime - startTime);
        } catch (Exception e) {
            MeteorClient.LOG.error("Failed to load modules", e);
        }
    }

    public void sortModules() {
        for (List<Module> modules : groups.values()) {
            modules.sort(Comparator.comparing(o -> o.title));
        }
    }

    public static void registerCategory(Category category) {
        if (!Categories.REGISTERING) throw new RuntimeException("Modules.registerCategory - Cannot register category outside of onRegisterCategories callback.");

        CATEGORIES.add(category);
    }

    public static Iterable<Category> loopCategories() {
        return CATEGORIES;
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> klass) {
        return (T) moduleInstances.get(klass);
    }

    public Module get(String name) {
        for (Module module : moduleInstances.values()) {
            if (module.name.equalsIgnoreCase(name)) return module;
        }

        return null;
    }

    public boolean isActive(Class<? extends Module> klass) {
        Module module = get(klass);
        return module != null && module.isActive();
    }

    public List<Module> getGroup(Category category) {
        return groups.computeIfAbsent(category, category1 -> new ArrayList<>());
    }

    public Collection<Module> getAll() {
        return moduleInstances.values();
    }

    public int getCount() {
        return moduleInstances.size();
    }

    public List<Module> getActive() {
        synchronized (active) {
            return active;
        }
    }

    public Set<Module> searchTitles(String text) {
        Map<Module, Integer> modules = new ValueComparableMap<>(Comparator.naturalOrder());

        for (Module module : this.moduleInstances.values()) {
            int score = Utils.searchLevenshteinDefault(module.title, text, false);
            if (Config.get().moduleAliases.get()) {
                for (String alias : module.aliases) {
                    int aliasScore = Utils.searchLevenshteinDefault(alias, text, false);
                    if (aliasScore < score) score = aliasScore;
                }
            }
            modules.put(module, modules.getOrDefault(module, 0) + score);
        }

        return modules.keySet();
    }

    public Set<Module> searchSettingTitles(String text) {
        Map<Module, Integer> modules = new ValueComparableMap<>(Comparator.naturalOrder());

        for (Module module : this.moduleInstances.values()) {
            int lowest = Integer.MAX_VALUE;
            for (SettingGroup sg : module.settings) {
                for (Setting<?> setting : sg) {
                    int score = Utils.searchLevenshteinDefault(setting.title, text, false);
                    if (score < lowest) lowest = score;
                }
            }
            modules.put(module, modules.getOrDefault(module, 0) + lowest);
        }

        return modules.keySet();
    }

    void addActive(Module module) {
        synchronized (active) {
            if (!active.contains(module)) {
                active.add(module);
                MeteorClient.EVENT_BUS.post(ActiveModulesChangedEvent.get());
            }
        }
    }

    void removeActive(Module module) {
        synchronized (active) {
            if (active.remove(module)) {
                MeteorClient.EVENT_BUS.post(ActiveModulesChangedEvent.get());
            }
        }
    }

    // Binding

    public void setModuleToBind(Module moduleToBind) {
        this.moduleToBind = moduleToBind;
    }

    /***
     * @see meteordevelopment.meteorclient.commands.commands.BindCommand
     * For ensuring we don't instantly bind the module to the enter key.
     */
    public void awaitKeyRelease() {
        this.awaitingKeyRelease = true;
    }

    public boolean isBinding() {
        return moduleToBind != null;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onKeyBinding(KeyEvent event) {
        if (event.action == KeyAction.Release && onBinding(true, event.key, event.modifiers)) event.cancel();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onButtonBinding(MouseButtonEvent event) {
        if (event.action == KeyAction.Release && onBinding(false, event.button, 0)) event.cancel();
    }

    private boolean onBinding(boolean isKey, int value, int modifiers) {
        if (!isBinding()) return false;

        if (awaitingKeyRelease) {
            if (!isKey || (value != GLFW.GLFW_KEY_ENTER && value != GLFW.GLFW_KEY_KP_ENTER)) return false;

            awaitingKeyRelease = false;
            return false;
        }

        if (moduleToBind.keybind.canBindTo(isKey, value, modifiers)) {
            moduleToBind.keybind.set(isKey, value, modifiers);
            moduleToBind.info("Bound to (highlight)%s(default).", moduleToBind.keybind);
        } else if (value == GLFW.GLFW_KEY_ESCAPE) {
            moduleToBind.keybind.set(Keybind.none());
            moduleToBind.info("Removed bind.");
        } else return false;

        MeteorClient.EVENT_BUS.post(ModuleBindChangedEvent.get(moduleToBind));
        moduleToBind = null;

        return true;
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onKey(KeyEvent event) {
        if (event.action == KeyAction.Repeat) return;
        onAction(true, event.key, event.modifiers, event.action == KeyAction.Press);
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onMouseButton(MouseButtonEvent event) {
        if (event.action == KeyAction.Repeat) return;
        onAction(false, event.button, 0, event.action == KeyAction.Press);
    }

    private void onAction(boolean isKey, int value, int modifiers, boolean isPress) {
        if (mc.currentScreen != null || Input.isKeyPressed(GLFW.GLFW_KEY_F3)) return;

        for (Module module : moduleInstances.values()) {
            if (module.keybind.matches(isKey, value, modifiers) && (isPress || (module.toggleOnBindRelease && module.isActive()))) {
                module.toggle();
                module.sendToggledMsg();
            }
        }
    }

    // End of binding

    @EventHandler(priority = EventPriority.HIGHEST + 1)
    private void onOpenScreen(OpenScreenEvent event) {
        if (!Utils.canUpdate()) return;

        for (Module module : moduleInstances.values()) {
            if (module.toggleOnBindRelease && module.isActive()) {
                module.toggle();
                module.sendToggledMsg();
            }
        }
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        synchronized (active) {
            for (Module module : getAll()) {
                if (module.isActive() && !module.runInMainMenu) {
                    MeteorClient.EVENT_BUS.subscribe(module);
                    module.onActivate();
                }
            }
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        synchronized (active) {
            for (Module module : getAll()) {
                if (module.isActive() && !module.runInMainMenu) {
                    MeteorClient.EVENT_BUS.unsubscribe(module);
                    module.onDeactivate();
                }
            }
        }
    }

    public void disableAll() {
        synchronized (active) {
            for (Module module : getAll()) {
                if (module.isActive()) module.toggle();
            }
        }
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        NbtList modulesTag = new NbtList();
        for (Module module : getAll()) {
            NbtCompound moduleTag = module.toTag();
            if (moduleTag != null) modulesTag.add(moduleTag);
        }
        tag.put("modules", modulesTag);

        return tag;
    }

    @Override
    public Modules fromTag(NbtCompound tag) {
        disableAll();

        NbtList modulesTag = tag.getListOrEmpty("modules");
        for (NbtElement moduleTagI : modulesTag) {
            NbtCompound moduleTag = (NbtCompound) moduleTagI;
            Module module = get(moduleTag.getString("name", ""));
            if (module != null) module.fromTag(moduleTag);
        }

        return this;
    }

    // INIT MODULES

    public void add(Module module) {
        // Check if the module's category is registered
        if (!CATEGORIES.contains(module.category)) {
            throw new RuntimeException("Modules.addModule - Module's category was not registered.");
        }

        // Remove the previous module with the same name
        AtomicReference<Module> removedModule = new AtomicReference<>();
        if (moduleInstances.values().removeIf(module1 -> {
            if (module1.name.equals(module.name)) {
                removedModule.set(module1);
                module1.settings.unregisterColorSettings();

                return true;
            }

            return false;
        })) {
            getGroup(removedModule.get().category).remove(removedModule.get());
        }

        // Add the module
        moduleInstances.put(module.getClass(), module);
        getGroup(module.category).add(module);

        // Register color settings for the module
        module.settings.registerColorSettings(module);
    }
}
