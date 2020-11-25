/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules;

import com.mojang.serialization.Lifecycle;
import me.zero.alpine.event.EventPriority;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.events.GameDisconnectedEvent;
import minegame159.meteorclient.events.GameJoinedEvent;
import minegame159.meteorclient.events.KeyEvent;
import minegame159.meteorclient.modules.combat.*;
import minegame159.meteorclient.modules.misc.Timer;
import minegame159.meteorclient.modules.misc.*;
import minegame159.meteorclient.modules.movement.*;
import minegame159.meteorclient.modules.player.*;
import minegame159.meteorclient.modules.render.*;
import minegame159.meteorclient.modules.render.hud.HUD;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.*;
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
import java.io.File;
import java.util.*;

public class ModuleManager extends Savable<ModuleManager> implements Listenable {
    public static final Category[] CATEGORIES = { Category.Combat, Category.Player, Category.Movement, Category.Render, Category.Misc };
    public static ModuleManager INSTANCE;
    public static final ModuleRegistry REGISTRY = new ModuleRegistry();

    private final Map<Class<? extends Module>, Module> modules = new HashMap<>();
    private final Map<Category, List<Module>> groups = new HashMap<>();

    private final List<ToggleModule> active = new ArrayList<>();
    private Module moduleToBind;

    public boolean onKeyOnlyBinding = false;

    public ModuleManager() {
        super(new File(MeteorClient.FOLDER, "modules.nbt"));

        INSTANCE = this;

        initCombat();
        initPlayer();
        initMovement();
        initRender();
        initMisc();

        for (List<Module> modules : groups.values()) {
            modules.sort(Comparator.comparing(o -> o.title));
        }

        MeteorClient.EVENT_BUS.subscribe(this);
    }

    public <T extends Module> T get(Class<T> klass) {
        return (T) modules.get(klass);
    }

    public Module get(String name) {
        for (Module module : modules.values()) {
            if (module.name.equalsIgnoreCase(name)) return module;
        }

        return null;
    }

    public boolean isActive(Class<? extends ToggleModule> klass) {
        ToggleModule module = get(klass);
        return module != null && module.isActive();
    }

    public List<Module> getGroup(Category category) {
        return groups.computeIfAbsent(category, category1 -> new ArrayList<>());
    }

    public Collection<Module> getAll() {
        return modules.values();
    }

    public List<ToggleModule> getActive() {
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

    void addActive(ToggleModule module) {
        synchronized (active) {
            if (!active.contains(module)) {
                active.add(module);
                MeteorClient.EVENT_BUS.post(EventStore.activeModulesChangedEvent());
            }
        }
    }

    void removeActive(ToggleModule module) {
        synchronized (active) {
            if (active.remove(module)) {
                MeteorClient.EVENT_BUS.post(EventStore.activeModulesChangedEvent());
            }
        }
    }

    @EventHandler
    public Listener<KeyEvent> onKey = new Listener<>(event -> {
        if (event.action == KeyAction.Repeat) return;

        // Check if binding module
        if (event.action == KeyAction.Press && moduleToBind != null) {
            moduleToBind.setKey(event.key);
            Chat.info("Module (highlight)%s (default)bound to (highlight)%s(default).", moduleToBind.title, Utils.getKeyName(event.key));
            moduleToBind = null;
            event.cancel();
            return;
        }

        // Find module bound to that key
        if (!onKeyOnlyBinding && MinecraftClient.getInstance().currentScreen == null && !Input.isPressed(GLFW.GLFW_KEY_F3)) {
            for (Module module : modules.values()) {
                if (module.getKey() == event.key && (event.action == KeyAction.Press || module.toggleOnKeyRelease)) {
                    module.doAction();
                    if (module instanceof ToggleModule) ((ToggleModule) module).sendToggledMsg();

                    save();
                }
            }
        }
    }, EventPriority.HIGHEST + 1);

    @EventHandler
    private final Listener<GameJoinedEvent> onGameJoined = new Listener<>(event -> {
        synchronized (active) {
            for (ToggleModule module : active) {
                MeteorClient.EVENT_BUS.subscribe(module);
                module.onActivate();
            }
        }
    });

    @EventHandler
    private final Listener<GameDisconnectedEvent> onGameDisconnected = new Listener<>(event -> {
        synchronized (active) {
            for (ToggleModule module : active) {
                MeteorClient.EVENT_BUS.unsubscribe(module);
                module.onDeactivate();
            }
        }
    });

    public void disableAll() {
        synchronized (active) {
            for (ToggleModule module : active.toArray(new ToggleModule[0])) {
                module.toggle();
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
    public ModuleManager fromTag(CompoundTag tag) {
        ListTag modulesTag = tag.getList("modules", 10);
        for (Tag moduleTagI : modulesTag) {
            CompoundTag moduleTag = (CompoundTag) moduleTagI;
            Module module = get(moduleTag.getString("name"));
            if (module != null) module.fromTag(moduleTag);
        }

        return this;
    }

    // INIT MODULES

    private void addModule(Module module) {
        modules.put(module.getClass(), module);
        getGroup(module.category).add(module);
    }

    private void initCombat() {
        addModule(new Auto32K());
        addModule(new AntiFriendHit());
        addModule(new Criticals());
        addModule(new AutoTotem());
        addModule(new BedAura());
        addModule(new AutoWeapon());
        addModule(new AutoLog());
        addModule(new KillAura());
        addModule(new CrystalAura());
        addModule(new OffhandExtra());
        addModule(new SmartSurround());
        addModule(new Surround());
        addModule(new Trigger());
        addModule(new AimAssist());
        addModule(new AutoArmor());
        addModule(new AntiBed());
        addModule(new AnchorAura());
        addModule(new TotemPopNotifier());
        addModule(new BowSpam());
        addModule(new AutoTrap());
        addModule(new AutoAnvil());
        addModule(new SelfTrap());
        addModule(new SelfWeb());
        addModule(new AutoWeb());
        addModule(new HoleFiller());
    }

    private void initPlayer() {
        addModule(new AutoFish());
        addModule(new DeathPosition());
        addModule(new FastUse());
        addModule(new AutoRespawn());
        addModule(new AntiFire());
        addModule(new AutoMend());
        addModule(new AutoGap());
        addModule(new AutoReplenish());
        addModule(new AntiHunger());
        addModule(new AutoTool());
        addModule(new AutoEat());
        addModule(new AutoMount());
        addModule(new XpBottleThrower());
        addModule(new MiddleClickExtra());
        addModule(new AutoDrop());
        addModule(new AutoRightClick());
        addModule(new Portals());
        addModule(new Reach());
        addModule(new PotionSpoof());
        addModule(new LiquidInteract());
        addModule(new MountBypass());
        addModule(new PacketMine());
        addModule(new XCarry());
        addModule(new BuildHeight());
        addModule(new Rotation());
        addModule(new ChestSwap());
        addModule(new NoMiningTrace());
        addModule(new EndermanLook());
        addModule(new NoBreakDelay());
        addModule(new AntiCactus());
    }

    private void initMovement() {
        addModule(new AutoSprint());
        addModule(new AutoWalk());
        addModule(new Blink());
        addModule(new FastLadder());
        addModule(new NoFall());
        addModule(new Spider());
        addModule(new AutoJump());
        addModule(new Flight());
        addModule(new Velocity());
        addModule(new ElytraPlus());
        addModule(new EntityControl());
        addModule(new HighJump());
        addModule(new Speed());
        addModule(new SafeWalk());
        addModule(new Parkour());
        addModule(new Step());
        addModule(new Jesus());
        addModule(new AirJump());
        addModule(new AntiLevitation());
        addModule(new Scaffold());
        addModule(new BoatFly());
        addModule(new NoSlow());
        addModule(new InvMove());
        addModule(new Anchor());
        addModule(new ClickTP());
    }

    private void initRender() {
        addModule(new HUD());
        addModule(new FullBright());
        addModule(new StorageESP());
        addModule(new XRay());
        addModule(new ESP());
        addModule(new Freecam());
        addModule(new Tracers());
        addModule(new Nametags());
        addModule(new HoleESP());
        addModule(new LogoutSpots());
        addModule(new Trajectories());
        addModule(new Chams());
        addModule(new CameraClip());
        addModule(new Search());
        addModule(new EntityOwner());
        addModule(new NoRender());
        addModule(new Breadcrumbs());
        addModule(new BlockSelection());
        addModule(new CustomFOV());
        addModule(new HandView());
    }

    private void initMisc() {
        addModule(new AutoSign());
        addModule(new AutoReconnect());
        addModule(new ShulkerTooltip());
        addModule(new AutoShearer());
        addModule(new AutoNametag());
        addModule(new BookBot());
        addModule(new DiscordPresence());
        addModule(new EChestFarmer());
        addModule(new MiddleClickFriend());
        addModule(new StashFinder());
        addModule(new AutoBrewer());
        addModule(new AutoSmelter());
        addModule(new Annoy());
        addModule(new Spam());
        addModule(new UnfocusedCPU());
        addModule(new ItemByteSize());
        addModule(new PacketCanceller());
        addModule(new EntityLogger());
        addModule(new EChestPreview());
        addModule(new Timer());
        addModule(new Suffix());
        addModule(new MessageAura());
        addModule(new Nuker());
        addModule(new SoundBlocker());
        addModule(new AntiChunkBan());
        addModule(new Announcer());
        addModule(new BetterChat());
        addModule(new FancyChat());
        addModule(new OffHandCrash());
        addModule(new LiquidFiller());
    }

    public static class ModuleRegistry extends Registry<ToggleModule> {
        public ModuleRegistry() {
            super(RegistryKey.ofRegistry(new Identifier("meteor-client", "modules")), Lifecycle.stable());
        }

        @Nullable
        @Override
        public Identifier getId(ToggleModule entry) {
            return null;
        }

        @Override
        public Optional<RegistryKey<ToggleModule>> getKey(ToggleModule entry) {
            return Optional.empty();
        }

        @Override
        public int getRawId(@Nullable ToggleModule entry) {
            return 0;
        }

        @Nullable
        @Override
        public ToggleModule get(@Nullable RegistryKey<ToggleModule> key) {
            return null;
        }

        @Nullable
        @Override
        public ToggleModule get(@Nullable Identifier id) {
            return null;
        }

        @Override
        protected Lifecycle getEntryLifecycle(ToggleModule object) {
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
        public Set<Map.Entry<RegistryKey<ToggleModule>, ToggleModule>> getEntries() {
            return null;
        }

        @Override
        public boolean containsId(Identifier id) {
            return false;
        }

        @Nullable
        @Override
        public ToggleModule get(int index) {
            return null;
        }

        @Override
        public Iterator<ToggleModule> iterator() {
            return new ToggleModuleIterator();
        }

        private static class ToggleModuleIterator implements Iterator<ToggleModule> {
            private final Iterator<Module> iterator = ModuleManager.INSTANCE.getAll().iterator();

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public ToggleModule next() {
                return (ToggleModule) iterator.next();
            }
        }
    }
}
