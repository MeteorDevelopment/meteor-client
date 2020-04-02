package minegame159.meteorclient.modules;

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
import minegame159.meteorclient.modules.misc.*;
import minegame159.meteorclient.modules.movement.*;
import minegame159.meteorclient.modules.player.*;
import minegame159.meteorclient.modules.render.*;
import minegame159.meteorclient.modules.setting.*;
import minegame159.meteorclient.utils.Savable;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.io.File;
import java.util.*;

public class ModuleManager extends Savable<ModuleManager> implements Listenable {
    public static final Category[] CATEGORIES = { Category.Combat, Category.Player, Category.Movement, Category.Render, Category.Misc, Category.Setting };
    public static final ModuleManager INSTANCE = new ModuleManager();

    private Map<Class<? extends Module>, Module> modules = new HashMap<>();
    private Map<Category, List<Module>> groups = new HashMap<>();

    private List<ToggleModule> active = new ArrayList<>();
    private Module moduleToBind;

    private ModuleManager() {
        super(new File(MeteorClient.FOLDER, "modules.nbt"));

        initCombat();
        initPlayer();
        initMovement();
        initRender();
        initMisc();
        initSetting();

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
        return get(klass).isActive();
    }

    public List<Module> getGroup(Category category) {
        return groups.computeIfAbsent(category, category1 -> new ArrayList<>());
    }

    public Collection<Module> getAll() {
        return modules.values();
    }

    public List<ToggleModule> getActive() {
        return active;
    }

    public void setModuleToBind(Module moduleToBind) {
        this.moduleToBind = moduleToBind;
    }

    void addActive(ToggleModule module) {
        if (!active.contains(module)) {
            active.add(module);
            MeteorClient.EVENT_BUS.post(EventStore.activeModulesChangedEvent());
        }
    }

    void removeActive(ToggleModule module) {
        if (active.remove(module)) {
            MeteorClient.EVENT_BUS.post(EventStore.activeModulesChangedEvent());
        }
    }

    @EventHandler
    private Listener<KeyEvent> onKey = new Listener<>(event -> {
        if (!event.push) return;

        // Check if binding module
        if (moduleToBind != null) {
            moduleToBind.setKey(event.key);
            Utils.sendMessage("#yellowModule #blue'%s' #yellowbound to #blue%s#yellow.", moduleToBind.title, Utils.getKeyName(event.key));
            moduleToBind = null;
            event.cancel();
            return;
        }

        // Find module bound to that key
        for (Module module : modules.values()) {
            if (module.getKey() == event.key) {
                module.doAction();
                event.cancel();
            }
        }
    }, EventPriority.HIGHEST + 1);

    @EventHandler
    private Listener<GameJoinedEvent> onGameJoined = new Listener<>(event -> {
        for (ToggleModule module : active) module.onActivate();
    });

    @EventHandler
    private Listener<GameDisconnectedEvent> onGameDisconnected = new Listener<>(event -> {
        for (ToggleModule module : active) module.onDeactivate();
    });

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
        addModule(new Criticals());
        addModule(new AutoTotem());
        addModule(new AutoLog());
        addModule(new KillAura());
        addModule(new CrystalAura());
        addModule(new Surround());
    }

    private void initPlayer() {
        addModule(new AutoFish());
        addModule(new DeathPosition());
        addModule(new FastUse());
        addModule(new AutoRespawn());
        addModule(new AntiFire());
        addModule(new AutoMend());
        addModule(new AntiHunger());
        addModule(new AutoTool());
        addModule(new AutoEat());
        addModule(new XpBottleThrower());
        addModule(new AutoArmor());
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
        addModule(new NoPush());
        addModule(new ElytraPlus());
        addModule(new HighJump());
        addModule(new Speed());
        addModule(new SafeWalk());
        addModule(new Parkour());
    }

    private void initRender() {
        addModule(new HUD());
        addModule(new FullBright());
        addModule(new StorageESP());
        addModule(new XRay());
        addModule(new AntiFog());
        addModule(new NoHurtCam());
        addModule(new ESP());
        addModule(new Freecam());
        addModule(new Tracers());
        addModule(new Nametags());
        addModule(new InventoryViewer());
        addModule(new HoleESP());
        addModule(new LogoutSpot());
        addModule(new Trajectories());
    }

    private void initMisc() {
        addModule(new LongerChat());
        addModule(new AutoSign());
        addModule(new AntiWeather());
        addModule(new AutoReconnect());
        addModule(new ShulkerTooltip());
        addModule(new AutoShearer());
        addModule(new AutoNametag());
        addModule(new MiddleClickFriend());
        addModule(new StashFinder());
        addModule(new AutoBrewer());
        addModule(new AutoSmelter());
        addModule(new Annoy());
        addModule(new Spam());
    }

    private void initSetting() {
        addModule(new ConfigM());
        addModule(new GUI());
        addModule(new Friends());
        addModule(new Macros());
        addModule(new Baritone());
    }
}
