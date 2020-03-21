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
import minegame159.meteorclient.utils.Utils;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleManager implements Listenable {
    public static ModuleManager INSTANCE;
    public static final Category[] CATEGORIES = { Category.Combat, Category.Player, Category.Movement, Category.Render, Category.Misc, Category.Setting };

    private List<Module> modules = new ArrayList<>();
    private Map<Category, List<Module>> groups = new HashMap<>();
    private List<Module> active = new ArrayList<>();
    private Module moduleToBind;

    public ModuleManager() {
        initCombat();
        initPlayer();
        initMovement();
        initRender();
        initMisc();
        initSetting();
    }

    public List<Module> getGroup(Category category) {
        return groups.computeIfAbsent(category, category1 -> new ArrayList<>());
    }

    public List<Module> getAll() {
        return modules;
    }

    public List<Module> getActive() {
        return active;
    }

    public <T extends Module> T get(Class<T> klass) {
        for (Module module : modules) {
            if (module.getClass() == klass) return (T) module;
        }

        return null;
    }

    public Module get(String name) {
        for (Module module : modules) {
            if (module.name.equalsIgnoreCase(name)) return module;
        }

        return null;
    }

    public void setModuleToBind(Module moduleToBind) {
        this.moduleToBind = moduleToBind;
    }

    @EventHandler
    private Listener<KeyEvent> onKey = new Listener<>(event -> {
        if (!event.push) return;

        // Check if binding module
        if (moduleToBind != null) {
            if (moduleToBind.setting) {
                moduleToBind = null;
                return;
            }

            moduleToBind.setKey(event.key);
            Utils.sendMessage("#yellowModule #blue'%s' #yellowbound to #blue%s#yellow.", moduleToBind.title, GLFW.glfwGetKeyName(event.key, 0));
            moduleToBind = null;
            event.cancel();
            return;
        }

        // Find module bound to that key
        for (Module module : modules) {
            if (module.setting) continue;

            if (module.getKey() == event.key) {
                module.toggle();
                event.cancel();
            }
        }
    }, EventPriority.HIGHEST + 1);

    void addActive(Module module) {
        active.add(module);
        MeteorClient.eventBus.post(EventStore.activeModulesChangedEvent());
    }

    void removeActive(Module module) {
        active.remove(module);
        MeteorClient.eventBus.post(EventStore.activeModulesChangedEvent());
    }

    private void addModule(Module module) {
        modules.add(module);
        getGroup(module.category).add(module);
    }

    @EventHandler
    private Listener<GameJoinedEvent> onGameJoined = new Listener<>(event -> {
        for (Module module : active) module.onActivate();
    });

    @EventHandler
    private Listener<GameDisconnectedEvent> onGameDisconnected = new Listener<>(event -> {
        for (Module module : active) module.onDeactivate();
    });

    private void initCombat() {
        addModule(new Criticals());
        addModule(new AutoTotem());
        addModule(new AutoLog());
        addModule(new KillAura());
        addModule(new CrystalAura());
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
    }

    private void initMisc() {
        addModule(new LongerChat());
        addModule(new AutoSign());
        addModule(new AntiWeather());
        addModule(new AutoReconnect());
        addModule(new ShulkerTooltip());
        addModule(new AutoShearer());
        addModule(new AutoNametag());
        addModule(new AutoBreeder());
        addModule(new MiddleClickFriend());
        addModule(new StashRecorder());
        addModule(new AutoBrewer());
        addModule(new AutoSmelter());
    }

    private void initSetting() {
        addModule(new ConfigM());
        addModule(new GUI());
        addModule(new Friends());
        addModule(new Macros());
        addModule(new Baritone());
    }
}
