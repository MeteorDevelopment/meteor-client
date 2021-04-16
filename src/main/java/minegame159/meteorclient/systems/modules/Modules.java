/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules;

import com.mojang.serialization.Lifecycle;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.game.GameJoinedEvent;
import minegame159.meteorclient.events.game.GameLeftEvent;
import minegame159.meteorclient.events.game.OpenScreenEvent;
import minegame159.meteorclient.events.meteor.ActiveModulesChangedEvent;
import minegame159.meteorclient.events.meteor.KeyEvent;
import minegame159.meteorclient.events.meteor.ModuleBindChangedEvent;
import minegame159.meteorclient.events.meteor.MouseButtonEvent;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.System;
import minegame159.meteorclient.systems.Systems;
import minegame159.meteorclient.systems.modules.combat.*;
import minegame159.meteorclient.systems.modules.misc.*;
import minegame159.meteorclient.systems.modules.movement.*;
import minegame159.meteorclient.systems.modules.movement.elytrafly.ElytraFly;
import minegame159.meteorclient.systems.modules.movement.speed.Speed;
import minegame159.meteorclient.systems.modules.player.*;
import minegame159.meteorclient.systems.modules.render.*;
import minegame159.meteorclient.systems.modules.render.hud.HUD;
import minegame159.meteorclient.systems.modules.render.search.Search;
import minegame159.meteorclient.systems.modules.world.Timer;
import minegame159.meteorclient.systems.modules.world.*;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.misc.input.Input;
import minegame159.meteorclient.utils.misc.input.KeyAction;
import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Modules extends System<Modules> {
    public static final ModuleRegistry REGISTRY = new ModuleRegistry();

    private static final List<Category> CATEGORIES = new ArrayList<>();
    public static boolean REGISTERING_CATEGORIES;

    private final List<Module> modules = new ArrayList<>();
    private final Map<Class<? extends Module>, Module> moduleInstances = new HashMap<>();
    private final Map<Category, List<Module>> groups = new HashMap<>();

    private final List<Module> active = new ArrayList<>();
    private Module moduleToBind;

    public Modules() {
        super("modules");
    }

    public static Modules get() {
        return Systems.get(Modules.class);
    }

    @Override
    public void init() {
        initCombat();
        initPlayer();
        initMovement();
        initRender();
        initWorld();
        initMisc();
    }

    public void sortModules() {
        for (List<Module> modules : groups.values()) {
            modules.sort(Comparator.comparing(o -> o.title));
        }
        modules.sort(Comparator.comparing(o -> o.title));
    }

    public static void registerCategory(Category category) {
        if (!REGISTERING_CATEGORIES) throw new RuntimeException("Modules.registerCategory - Cannot register category outside of onRegisterCategories callback.");

        CATEGORIES.add(category);
    }

    public static Iterable<Category> loopCategories() {
        return CATEGORIES;
    }

    public static Category getCategoryByHash(int hash) {
        for (Category category : CATEGORIES) {
            if (category.hashCode() == hash) return category;
        }

        return null;
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

    public List<Module> getList() {
        return modules;
    }

    public int getCount() {
        return moduleInstances.values().size();
    }

    public List<Module> getActive() {
        synchronized (active) {
            return active;
        }
    }

    public List<Pair<Module, Integer>> searchTitles(String text) {
        List<Pair<Module, Integer>> modules = new ArrayList<>();

        for (Module module : this.moduleInstances.values()) {
            int words = Utils.search(module.title, text);
            if (words > 0) modules.add(new Pair<>(module, words));
        }

        modules.sort(Comparator.comparingInt(value -> -value.getRight()));
        return modules;
    }

    public List<Pair<Module, Integer>> searchSettingTitles(String text) {
        List<Pair<Module, Integer>> modules = new ArrayList<>();

        for (Module module : this.moduleInstances.values()) {
            for (SettingGroup sg : module.settings) {
                for (Setting<?> setting : sg) {
                    int words = Utils.search(setting.title, text);
                    if (words > 0) {
                        modules.add(new Pair<>(module, words));
                        break;
                    }
                }
            }
        }

        modules.sort(Comparator.comparingInt(value -> -value.getRight()));
        return modules;
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

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onKeyBinding(KeyEvent event) {
        if (onBinding(true, event.key)) event.cancel();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onButtonBinding(MouseButtonEvent event) {
        if (onBinding(false, event.button)) event.cancel();
    }

    private boolean onBinding(boolean isKey, int value) {
        if (moduleToBind != null && moduleToBind.keybind.canBindTo(isKey, value)) {
            if (value != GLFW.GLFW_KEY_ESCAPE) {
                moduleToBind.keybind.set(isKey, value);
                ChatUtils.prefixInfo("KeyBinds", "Module (highlight)%s (default)bound to (highlight)%s(default).", moduleToBind.title, moduleToBind.keybind);
            }

            MeteorClient.EVENT_BUS.post(ModuleBindChangedEvent.get(moduleToBind));
            moduleToBind = null;
            return true;
        }

        return false;
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onKey(KeyEvent event) {
        if (event.action == KeyAction.Repeat) return;
        onAction(true, event.key, event.action == KeyAction.Press);
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onMouseButton(MouseButtonEvent event) {
        if (event.action == KeyAction.Repeat) return;
        onAction(false, event.button, event.action == KeyAction.Press);
    }

    private void onAction(boolean isKey, int value, boolean isPress) {
        if (MinecraftClient.getInstance().currentScreen == null && !Input.isKeyPressed(GLFW.GLFW_KEY_F3)) {
            for (Module module : moduleInstances.values()) {
                if (module.keybind.matches(isKey, value) && (isPress || module.toggleOnBindRelease)) {
                    module.doAction();
                    module.sendToggledMsg();
                }
            }
        }
    }

    // End of binding

    @EventHandler(priority = EventPriority.HIGHEST + 1)
    private void onOpenScreen(OpenScreenEvent event) {
        for (Module module : moduleInstances.values()) {
            if (module.toggleOnBindRelease) {
                if (module.isActive()) {
                    module.toggle();
                    module.sendToggledMsg();
                }
            }
        }
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        synchronized (active) {
            for (Module module : active) {
                MeteorClient.EVENT_BUS.subscribe(module);
                module.onActivate();
            }
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        synchronized (active) {
            for (Module module : active) {
                MeteorClient.EVENT_BUS.unsubscribe(module);
                module.onDeactivate();
            }
        }
    }

    public void disableAll() {
        synchronized (active) {
            for (Module module : active.toArray(new Module[0])) {
                module.toggle(Utils.canUpdate());
            }
        }
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        ListTag modulesTag = new ListTag();
        for (Module module : getAll()) {
            CompoundTag moduleTag = module.toTag();
            if (moduleTag != null) modulesTag.add(moduleTag);
        }
        tag.put("modules", modulesTag);

        return tag;
    }

    @Override
    public Modules fromTag(CompoundTag tag) {
        disableAll();

        ListTag modulesTag = tag.getList("modules", 10);
        for (Tag moduleTagI : modulesTag) {
            CompoundTag moduleTag = (CompoundTag) moduleTagI;
            Module module = get(moduleTag.getString("name"));
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
        modules.add(module);
        getGroup(module.category).add(module);

        // Register color settings for the module
        module.settings.registerColorSettings(module);
    }

    /** Only for backwards compatibility **/
    @Deprecated
    public void addModule(Module module) {
        add(module);
    }

    private void initCombat() {
        add(new AimAssist());
        add(new AnchorAura());
        add(new AntiAnvil());
        add(new AntiAnchor());
        add(new AntiBed());
        add(new AntiHit());
        add(new ArrowDodge());
        add(new Auto32K());
        add(new AutoAnvil());
        add(new AutoArmor());
        add(new AutoCity());
        add(new AutoLog());
        add(new AutoTotem());
        add(new AutoTrap());
        add(new AutoWeapon());
        add(new AutoWeb());
        add(new BedAura());
        add(new BowSpam());
        add(new Criticals());
        add(new CrystalAura());
        add(new Hitboxes());
        add(new HoleFiller());
        add(new KillAura());
        add(new OffhandExtra());
        add(new Quiver());
        add(new SelfAnvil());
        add(new SelfTrap());
        add(new SelfWeb());
        add(new SmartSurround());
        add(new Surround());
        add(new Swarm());
        add(new TotemPopNotifier());
        add(new Trigger());
        add(new Burrow());
    }

    private void initPlayer() {
        add(new AirPlace());
        add(new AntiAFK());
        add(new AntiHunger());
        add(new AutoClicker());
        add(new AutoDrop());
        add(new AutoFish());
        add(new AutoMend());
        add(new AutoReplenish());
        add(new AutoRespawn());
        add(new AutoTool());
        add(new ChestSwap());
        add(new DeathPosition());
        add(new EXPThrower());
        add(new FakePlayer());
        add(new FastUse());
        add(new GhostHand());
        add(new LiquidInteract());
        add(new MiddleClickExtra());
        add(new NameProtect());
        add(new NoBreakDelay());
        add(new NoInteract());
        add(new NoMiningTrace());
        add(new NoRotate());
        add(new Portals());
        add(new PotionSpoof());
        add(new Reach());
        add(new Rotation());
        add(new SpeedMine());
        add(new Trail());
        add(new XCarry());
        add(new AutoGap());
        add(new AutoEat());
        add(new PotionSaver());
    }

    private void initMovement() {
        add(new AirJump());
        add(new Anchor());
        add(new AntiLevitation());
        add(new AutoJump());
        add(new Sprint());
        add(new AutoWalk());
        add(new Blink());
        add(new BoatFly());
        add(new ClickTP());
        add(new ElytraBoost());
        add(new ElytraFly());
        add(new EntityControl());
        add(new EntitySpeed());
        add(new FastClimb());
        add(new Flight());
        add(new GUIMove());
        add(new HighJump());
        add(new Jesus());
        add(new NoFall());
        add(new NoSlow());
        add(new Parkour());
        add(new ReverseStep());
        add(new SafeWalk());
        add(new Scaffold());
        add(new Speed());
        add(new Spider());
        add(new Slippy());
        add(new Step());
        add(new Velocity());
        add(new AntiVoid());
    }

    private void initRender() {
        add(new BlockSelection());
        add(new Breadcrumbs());
        add(new BreakIndicators());
        add(new CameraClip());
        add(new Chams());
        add(new CityESP());
        add(new CustomFOV());
        add(new ESP());
        add(new EntityOwner());
        add(new FreeRotate());
        add(new Freecam());
        add(new Fullbright());
        add(new HUD());
        add(new HandView());
        add(new HoleESP());
        add(new ItemPhysics());
        add(new LogoutSpots());
        add(new Nametags());
        add(new NoRender());
        add(new ParticleBlocker());
        add(new Search());
        add(new StorageESP());
        add(new TimeChanger());
        add(new Tracers());
        add(new Trajectories());
        add(new UnfocusedCPU());
        add(new VoidESP());
        add(new Xray());
        add(new BossStack());
        add(new ItemHighlight());
        add(new LightOverlay());
        add(new Zoom());
        add(new WallHack());
        add(new WaypointsModule());
        add(new BetterTooltips());
    }

    private void initWorld() {
        add(new Ambience());
        add(new AntiCactus());
        add(new AutoBreed());
        add(new AutoBrewer());
        add(new AutoMount());
        add(new AutoNametag());
        add(new AutoShearer());
        add(new AutoSign());
        add(new AutoSmelter());
        add(new AutoSteal());
        add(new BuildHeight());
        add(new EChestFarmer());
        add(new EndermanLook());
        add(new EntityLogger());
        add(new Flamethrower());
        add(new InfinityMiner());
        add(new Nuker());
        add(new LiquidFiller());
        add(new MountBypass());
        add(new PacketMine());
        add(new StashFinder());
        add(new Timer());
        add(new VeinMiner());
        add(new InstaMine());
    }

    private void initMisc() {
        add(new Announcer());
        add(new AntiPacketKick());
        add(new AutoReconnect());
        add(new BetterChat());
        add(new BookBot());
        add(new DiscordPresence());
        add(new MessageAura());
        add(new MiddleClickFriend());
        add(new Notebot());
        add(new OffhandCrash());
        add(new PacketCanceller());
        add(new SoundBlocker());
        add(new Spam());
        add(new VisualRange());
        add(new VanillaSpoof());
        add(new AntiBot());
        add(new BetterTab());
    }

    public static class ModuleRegistry extends Registry<Module> {
        public ModuleRegistry() {
            super(RegistryKey.ofRegistry(new Identifier("meteor-client", "modules")), Lifecycle.stable());
        }

        @Nullable
        @Override
        public Identifier getId(Module entry) {
            return null;
        }

        @Override
        public Optional<RegistryKey<Module>> getKey(Module entry) {
            return Optional.empty();
        }

        @Override
        public int getRawId(@Nullable Module entry) {
            return 0;
        }

        @Nullable
        @Override
        public Module get(@Nullable RegistryKey<Module> key) {
            return null;
        }

        @Nullable
        @Override
        public Module get(@Nullable Identifier id) {
            return null;
        }

        @Override
        protected Lifecycle getEntryLifecycle(Module object) {
            return null;
        }

        @Override
        public Lifecycle getLifecycle() {
            return null;
        }

        @Override
        public Set<Identifier> getIds() {
            return null;
        }

        @Override
        public Set<Map.Entry<RegistryKey<Module>, Module>> getEntries() {
            return null;
        }

        @Override
        public boolean containsId(Identifier id) {
            return false;
        }

        @Nullable
        @Override
        public Module get(int index) {
            return null;
        }

        @Override
        public Iterator<Module> iterator() {
            return new ToggleModuleIterator();
        }

        private static class ToggleModuleIterator implements Iterator<Module> {
            private final Iterator<Module> iterator = Modules.get().getAll().iterator();

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Module next() {
                return iterator.next();
            }
        }
    }

}
