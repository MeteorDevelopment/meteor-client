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
import minegame159.meteorclient.settings.ColorSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.System;
import minegame159.meteorclient.systems.Systems;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.misc.input.Input;
import minegame159.meteorclient.utils.misc.input.KeyAction;
import minegame159.meteorclient.utils.player.ChatUtils;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.render.color.RainbowColors;
import minegame159.meteorclient.utils.render.color.SettingColor;
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

public class Modules extends System<Modules> {
    public static final Category[] CATEGORIES = {Category.Combat, Category.Player, Category.Movement, Category.Render, Category.Misc};
    public static final ModuleRegistry REGISTRY = new ModuleRegistry();

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

        for (List<Module> modules : groups.values()) {
            modules.sort(Comparator.comparing(o -> o.title));
        }
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

    public void addModule(Module module) {
        modules.put(module.getClass(), module);
        getGroup(module.category).add(module);

        for (SettingGroup group : module.settings) {
            for (Setting<?> setting : group) {
                setting.module = module;

                if (setting instanceof ColorSetting) {
                    RainbowColors.addSetting((Setting<SettingColor>) setting);
                }
            }
        }
    }

    private void initCombat() {
        addModule(new AimAssist());
        addModule(new AnchorAura());
        addModule(new AntiAnvil());
        addModule(new AntiAnchor());
        addModule(new AntiBed());
        addModule(new AntiFriendHit());
        addModule(new Auto32K());
        addModule(new AutoAnvil());
        addModule(new AutoArmor());
        addModule(new AutoCity());
        addModule(new AutoLog());
        addModule(new AutoTotem());
        addModule(new AutoTrap());
        addModule(new AutoWeapon());
        addModule(new AutoWeb());
        addModule(new BedAura());
        addModule(new BowSpam());
        addModule(new Criticals());
        addModule(new CrystalAura());
        addModule(new Hitboxes());
        addModule(new HoleFiller());
        addModule(new KillAura());
        addModule(new OffhandExtra());
        addModule(new Quiver());
        addModule(new SelfAnvil());
        addModule(new SelfTrap());
        addModule(new SelfWeb());
        addModule(new SmartSurround());
        addModule(new Surround());
        addModule(new Swarm());
        addModule(new TotemPopNotifier());
        addModule(new Trigger());
    }

    private void initPlayer() {
        addModule(new AirPlace());
        addModule(new AntiAFK());
        addModule(new AntiCactus());
        addModule(new AntiHunger());
        addModule(new AutoClicker());
        addModule(new AutoDrop());
        addModule(new AutoFish());
        addModule(new AutoMend());
        addModule(new AutoMount());
        addModule(new AutoReplenish());
        addModule(new AutoRespawn());
        addModule(new AutoTool());
        addModule(new BuildHeight());
        addModule(new ChestSwap());
        addModule(new DeathPosition());
        addModule(new EXPThrower());
        addModule(new EndermanLook());
        addModule(new FakePlayer());
        addModule(new FastUse());
        addModule(new GhostHand());
        addModule(new InfinityMiner());
        addModule(new LiquidInteract());
        addModule(new MiddleClickExtra());
        addModule(new MountBypass());
        addModule(new NameProtect());
        addModule(new NoBreakDelay());
        addModule(new NoInteract());
        addModule(new NoMiningTrace());
        addModule(new NoRotate());
        addModule(new PacketMine());
        addModule(new Portals());
        addModule(new PotionSpoof());
        addModule(new Reach());
        addModule(new Rotation());
        addModule(new SpeedMine());
        addModule(new Trail());
        addModule(new XCarry());
        addModule(new AutoGap());
        addModule(new AutoEat());
    }

    private void initMovement() {
        addModule(new AirJump());
        addModule(new Anchor());
        addModule(new AntiLevitation());
        addModule(new AutoJump());
        addModule(new Sprint());
        addModule(new AutoWalk());
        addModule(new Blink());
        addModule(new BoatFly());
        addModule(new ClickTP());
        addModule(new ElytraBoost());
        addModule(new ElytraFly());
        addModule(new EntityControl());
        addModule(new EntitySpeed());
        addModule(new FastClimb());
        addModule(new Flight());
        addModule(new GUIMove());
        addModule(new HighJump());
        addModule(new Jesus());
        addModule(new NoFall());
        addModule(new NoSlow());
        addModule(new Parkour());
        addModule(new ReverseStep());
        addModule(new SafeWalk());
        addModule(new Scaffold());
        addModule(new Speed());
        addModule(new Spider());
        addModule(new Step());
        addModule(new Timer());
        addModule(new Velocity());
    }

    private void initRender() {
        addModule(new BlockSelection());
        addModule(new Breadcrumbs());
        addModule(new BreakIndicators());
        addModule(new CameraClip());
        addModule(new Chams());
        addModule(new CityESP());
        addModule(new CustomFOV());
        addModule(new EChestPreview());
        addModule(new ESP());
        addModule(new EntityOwner());
        addModule(new FreeRotate());
        addModule(new Freecam());
        addModule(new Fullbright());
        addModule(new HUD());
        addModule(new HandView());
        addModule(new HoleESP());
        addModule(new ItemByteSize());
        addModule(new ItemPhysics());
        addModule(new LogoutSpots());
        addModule(new Nametags());
        addModule(new NoRender());
        addModule(new ParticleBlocker());
        addModule(new Search());
        addModule(new ShulkerPeek());
        addModule(new StorageESP());
        addModule(new TimeChanger());
        addModule(new Tracers());
        addModule(new Trajectories());
        addModule(new UnfocusedCPU());
        addModule(new VoidESP());
        addModule(new Xray());
        addModule(new BossStack());
        addModule(new ItemHighlight());
    }

    private void initMisc() {
        addModule(new Announcer());
        addModule(new AntiPacketKick());
        addModule(new AutoBreed());
        addModule(new AutoBrewer());
        addModule(new AutoNametag());
        addModule(new AutoReconnect());
        addModule(new AutoShearer());
        addModule(new AutoSign());
        addModule(new AutoSmelter());
        addModule(new AutoSteal());
        addModule(new BetterChat());
        addModule(new BookBot());
        addModule(new DiscordPresence());
        addModule(new EChestFarmer());
        addModule(new EntityLogger());
        addModule(new LiquidFiller());
        addModule(new MessageAura());
        addModule(new MiddleClickFriend());
        addModule(new Nuker());
        addModule(new OffhandCrash());
        addModule(new PacketCanceller());
        addModule(new SoundBlocker());
        addModule(new Spam());
        addModule(new StashFinder());
        addModule(new VisualRange());
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