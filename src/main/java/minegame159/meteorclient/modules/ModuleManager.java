package minegame159.meteorclient.modules;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.modules.misc.LongerChat;
import minegame159.meteorclient.modules.movement.*;
import minegame159.meteorclient.modules.player.AutoFish;
import minegame159.meteorclient.modules.player.DeathPosition;
import minegame159.meteorclient.modules.player.FastUse;
import minegame159.meteorclient.modules.render.*;
import minegame159.meteorclient.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ModuleManager {
    private static List<Module> modules = new ArrayList<>();
    private static List<Module> player = new ArrayList<>();
    private static List<Module> movement = new ArrayList<>();
    private static List<Module> render = new ArrayList<>();
    private static List<Module> misc = new ArrayList<>();

    private static List<Module> active = new ArrayList<>();

    public static Module moduleToBind;

    public static void init() {
        initPlayer();
        initMovement();
        initRender();
        initMisc();
        System.out.println("Meteor Client loaded " + modules.size() + " modules.");
    }

    public static boolean onKeyPress(int key) {
        // Check if binding module
        if (moduleToBind != null) {
            moduleToBind.key = key;
            moduleToBind = null;
            Utils.sendMessage("#yellowModule bound.");
            return true;
        }

        // Find module bound to that key
        for (Module module : modules) {
            if (module.key == key) {
                module.toggle();
                return true;
            }
        }

        return false;
    }

    public static Module get(String name) {
        name = name.toLowerCase();
        for (Module module : modules) {
            if (module.name.equals(name)) return module;
        }

        return null;
    }

    public static void forEachAll(Consumer<Module> consumer) {
        modules.forEach(consumer);
    }

    public static List<Module> getActive() {
        return active;
    }

    public static void playerForEach(Consumer<Module> consumer) {
        player.forEach(consumer);
    }
    public static int playerCount() {
        return player.size();
    }

    public static void movementForEach(Consumer<Module> consumer) {
        movement.forEach(consumer);
    }
    public static int movementCount() {
        return movement.size();
    }

    public static void renderForEach(Consumer<Module> consumer) {
        render.forEach(consumer);
    }
    public static int renderCount() {
        return render.size();
    }

    public static void miscForEach(Consumer<Module> consumer) {
        misc.forEach(consumer);
    }
    public static int miscCount() {
        return misc.size();
    }

    public static int getCount() {
        return modules.size();
    }

    public static void deactivateAll() {
        List<Module> active2 = new ArrayList<>(active);
        for (Module module : active2) module.toggle();
    }

    static void addActive(Module module) {
        active.add(module);
        MeteorClient.eventBus.post(EventStore.activeModulesChangedEvent());
    }
    static void removeActive(Module module) {
        active.remove(module);
        MeteorClient.eventBus.post(EventStore.activeModulesChangedEvent());
    }

    public static List<Module> getAll() {
        return modules;
    }

    private static void addModule(Module module) {
        switch (module.category) {
            case Player:   player.add(module); break;
            case Movement: movement.add(module); break;
            case Render:   render.add(module); break;
            case Misc:     misc.add(module); break;
        }
        modules.add(module);
    }

    private static void initMisc() {
        addModule(new LongerChat());
    }

    private static void initMovement() {
        addModule(new AutoSprint());
        addModule(new AutoWalk());
        addModule(new Blink());
        addModule(new FastLadder());
        addModule(new NoFall());
        addModule(new Spider());
        addModule(new AutoJump());
        addModule(new Flight());
    }

    private static void initPlayer() {
        addModule(new AutoFish());
        addModule(new DeathPosition());
        addModule(new FastUse());
    }

    private static void initRender() {
        addModule(new ActiveModules());
        addModule(new FullBright());
        addModule(new Info());
        addModule(new Position());
        addModule(new StorageESP());
        addModule(new XRay());
        addModule(new Chams());
        addModule(new AntiFog());
        addModule(new NoHurtCam());
    }
}
