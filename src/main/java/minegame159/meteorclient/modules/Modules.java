/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules;

import com.mojang.serialization.Lifecycle;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.game.GameJoinedEvent;
import minegame159.meteorclient.events.game.GameLeftEvent;
import minegame159.meteorclient.events.game.OpenScreenEvent;
import minegame159.meteorclient.events.meteor.ActiveModulesChangedEvent;
import minegame159.meteorclient.events.meteor.KeyEvent;
import minegame159.meteorclient.modules.combat.*;
import minegame159.meteorclient.modules.misc.*;
import minegame159.meteorclient.modules.movement.Timer;
import minegame159.meteorclient.modules.movement.*;
import minegame159.meteorclient.modules.movement.elytrafly.ElytraFly;
import minegame159.meteorclient.modules.movement.speed.Speed;
import minegame159.meteorclient.modules.player.*;
import minegame159.meteorclient.modules.render.*;
import minegame159.meteorclient.modules.render.hud.HUD;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.System;
import minegame159.meteorclient.systems.Systems;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.misc.input.Input;
import minegame159.meteorclient.utils.misc.input.KeyAction;
import minegame159.meteorclient.utils.player.ChatUtils;
import minegame159.meteorclient.utils.player.InvUtils;
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

    private final Map<Class<? extends Module>, Module> modules = new HashMap<>();
    private final Map<Category, List<Module>> groups = new HashMap<>();

    private final List<Module> active = new ArrayList<>();
    private Module moduleToBind;

    public boolean onKeyOnlyBinding = false;

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
        initMisc();
    }

    public void sortModules() {
        for (List<Module> modules : groups.values()) {
            modules.sort(Comparator.comparing(o -> o.title));
        }
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
        return (T) modules.get(klass);
    }

    public Module get(String name) {
        for (Module module : modules.values()) {
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
        return modules.values();
    }

    public List<Module> getActive() {
        synchronized (active) {
            return active;
        }
    }

    public void setModuleToBind(Module moduleToBind) {
        this.moduleToBind = moduleToBind;
    }

    public List<Pair<Module, Integer>> searchTitles(String text) {
        List<Pair<Module, Integer>> modules = new ArrayList<>();

        for (Module module : this.modules.values()) {
            int words = Utils.search(module.title, text);
            if (words > 0) modules.add(new Pair<>(module, words));
        }

        modules.sort(Comparator.comparingInt(value -> -value.getRight()));
        return modules;
    }

    public List<Pair<Module, Integer>> searchSettingTitles(String text) {
        List<Pair<Module, Integer>> modules = new ArrayList<>();

        for (Module module : this.modules.values()) {
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

    @EventHandler(priority = EventPriority.HIGHEST + 1)
    private void onKey(KeyEvent event) {
        if (event.action == KeyAction.Repeat) return;

        // Check if binding module
        if (event.action == KeyAction.Press && moduleToBind != null) {
            moduleToBind.setKey(event.key);
            ChatUtils.prefixInfo("KeyBinds", "Module (highlight)%s (default)bound to (highlight)%s(default).", moduleToBind.title, Utils.getKeyName(event.key));
            moduleToBind = null;
            event.cancel();
            return;
        }

        // Find module bound to that key
        if (!onKeyOnlyBinding && MinecraftClient.getInstance().currentScreen == null && !Input.isPressed(GLFW.GLFW_KEY_F3)) {
            for (Module module : modules.values()) {
                if (module.getKey() == event.key && (event.action == KeyAction.Press || module.toggleOnKeyRelease)) {
                    module.doAction();
                    module.sendToggledMsg();
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST + 1)
    private void onOpenScreen(OpenScreenEvent event) {
        for (Module module : modules.values()) {
            if (module.toggleOnKeyRelease) {
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
            MeteorClient.EVENT_BUS.subscribe(new InvUtils());
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
        if (modules.values().removeIf(module1 -> {
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
        modules.put(module.getClass(), module);
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
        add(new AntiFriendHit());
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
    }

    private void initPlayer() {
        add(new AirPlace());
        add(new AntiAFK());
        add(new AntiCactus());
        add(new AntiHunger());
        add(new AutoClicker());
        add(new AutoDrop());
        add(new AutoFish());
        add(new AutoMend());
        add(new AutoMount());
        add(new AutoReplenish());
        add(new AutoRespawn());
        add(new AutoTool());
        add(new BuildHeight());
        add(new ChestSwap());
        add(new DeathPosition());
        add(new EXPThrower());
        add(new EndermanLook());
        add(new FakePlayer());
        add(new FastUse());
        add(new GhostHand());
        add(new InfinityMiner());
        add(new LiquidInteract());
        add(new MiddleClickExtra());
        add(new MountBypass());
        add(new NameProtect());
        add(new NoBreakDelay());
        add(new NoInteract());
        add(new NoMiningTrace());
        add(new NoRotate());
        add(new PacketMine());
        add(new Portals());
        add(new PotionSpoof());
        add(new Reach());
        add(new Rotation());
        add(new SpeedMine());
        add(new Trail());
        add(new XCarry());
        add(new AutoGap());
        add(new AutoEat());
        add(new AutoPot());
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
        add(new Step());
        add(new Timer());
        add(new Velocity());
    }

    private void initRender() {
        add(new BlockSelection());
        add(new Breadcrumbs());
        add(new BreakIndicators());
        add(new CameraClip());
        add(new Chams());
        add(new CityESP());
        add(new CustomFOV());
        add(new EChestPreview());
        add(new ESP());
        add(new EntityOwner());
        add(new FreeRotate());
        add(new Freecam());
        add(new Fullbright());
        add(new HUD());
        add(new HandView());
        add(new HoleESP());
        add(new ItemByteSize());
        add(new ItemPhysics());
        add(new LogoutSpots());
        add(new Nametags());
        add(new NoRender());
        add(new ParticleBlocker());
        add(new Search());
        add(new ShulkerPeek());
        add(new StorageESP());
        add(new TimeChanger());
        add(new Tracers());
        add(new Trajectories());
        add(new UnfocusedCPU());
        add(new VoidESP());
        add(new Xray());
        add(new BossStack());
        add(new ItemHighlight());
        add(new Ambience());
    }

    private void initMisc() {
        add(new Announcer());
        add(new AntiPacketKick());
        add(new AutoBreed());
        add(new AutoBrewer());
        add(new AutoNametag());
        add(new AutoReconnect());
        add(new AutoShearer());
        add(new AutoSign());
        add(new AutoSmelter());
        add(new AutoSteal());
        add(new BetterChat());
        add(new BookBot());
        add(new DiscordPresence());
        add(new EChestFarmer());
        add(new EntityLogger());
        add(new LiquidFiller());
        add(new MessageAura());
        add(new MiddleClickFriend());
        add(new Nuker());
        add(new OffhandCrash());
        add(new PacketCanceller());
        add(new SoundBlocker());
        add(new Spam());
        add(new StashFinder());
        add(new VisualRange());
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