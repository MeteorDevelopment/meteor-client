package minegame159.meteorclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.zero.alpine.bus.EventBus;
import me.zero.alpine.bus.EventManager;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.commands.CommandManager;
import minegame159.meteorclient.events.GameJoinedEvent;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.gui.clickgui.ClickGUI;
import minegame159.meteorclient.json.ConfigSerializer;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.utils.EntityUtils;
import minegame159.meteorclient.utils.Utils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.FabricKeyBinding;
import net.fabricmc.fabric.api.client.keybinding.KeyBindingRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.*;

public class MeteorClient implements ClientModInitializer, Listenable {
    public static MeteorClient instance;
    public static EventBus eventBus = new EventManager();
    public static Gson gson;

    private static MinecraftClient mc;
    private static File configFile;
    private static FabricKeyBinding openClickGui = FabricKeyBinding.Builder.create(new Identifier("meteor-client", "open-click-gui"), InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "key.categories.misc").build();

    boolean init;

    @Override
    public void onInitializeClient() {
        if (!init) {
            init = true;
            instance = this;
            return;
        }

        System.out.println("Initializing Meteor Client.");

        mc = MinecraftClient.getInstance();
        Utils.mc = mc;
        EntityUtils.mc = mc;

        gson = new GsonBuilder()
                .registerTypeAdapter(Config.class, new ConfigSerializer())
                .setPrettyPrinting()
                .create();

        configFile = new File(FabricLoader.getInstance().getGameDirectory(), "meteor-client.json");

        MixinValues.init();
        CommandManager.init();
        ModuleManager.init();

        KeyBindingRegistry.INSTANCE.register(openClickGui);

        eventBus.subscribe(this);
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        if (openClickGui.isPressed() && !mc.isPaused()) {
            mc.openScreen(new ClickGUI());

            /*WidgetScreen screen = new WidgetScreen("Test");

            WPanel panel = screen.add(new WPanel());
            panel.boundingBox.alignment.set(Alignment.X.Center, Alignment.Y.Center);
            panel.boundingBox.setMargin(6);

            WVerticalList list = panel.add(new WVerticalList(4));
            list.maxHeight = MinecraftClient.getInstance().getWindow().getScaledHeight() - 64;
            list.scrollOnlyWhenMouseOver = false;

            WLabel title = list.add(new WLabel("Hello, World!", true));
            title.boundingBox.alignment.x = Alignment.X.Center;
            title.tooltip = "Nightmare, nightmare, nightmare!";
            list.add(new WHorizontalSeparator());

            WGrid grid = list.add(new WGrid(4, 4, 2));
            grid.addRow(new WLabel("Players:"), new WCheckbox(true));
            grid.addRow(new WLabel("Mobs:"), new WCheckbox(false));
            grid.addRow(new WLabel("Animals:"), new WCheckbox(true));
            grid.addRow(new WLabel("Items:"), new WCheckbox(false));
            grid.addRow(new WLabel("Vehicles:"), new WCheckbox(true));
            list.add(new WHorizontalSeparator());

            WHorizontalList name = list.add(new WHorizontalList(4));
            name.add(new WLabel("Name:"));
            name.add(new WTextBox("MineGame159", 16));

            WHorizontalList number = list.add(new WHorizontalList(4));
            number.add(new WLabel("Number:"));
            number.add(new WDoubleTextBox(8.63, 8));
            list.add(new WHorizontalSeparator());

            list.add(new WButton("Click Me!")).action = () -> System.out.println("Clicked!");
            list.add(new WEnumButton<>(Alignment.X.Left));
            list.add(new WHorizontalSeparator());

            list.add(new WLabel("Just some text"));
            list.add(new WLabel("Don't mind"));
            list.add(new WLabel("ME"));
            list.add(new WLabel("!!!!"));
            list.add(new WHorizontalSeparator());

            WHorizontalList color = list.add(new WHorizontalList(4));
            color.add(new WLabel("Color:"));
            color.add(new WColorEdit(new Color(255, 25, 25)));

            screen.layout();
            mc.openScreen(screen);*/
        }
    });

    @EventHandler
    private Listener<GameJoinedEvent> onGameJoined = new Listener<>(event -> MeteorClient.loadConfig());

    public static void saveConfig() {
        try {
            Writer writer = new FileWriter(configFile);
            gson.toJson(Config.instance, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadConfig() {
        if (configFile.exists()) {
            ModuleManager.deactivateAll();
            try {
                Config.instance = gson.fromJson(new FileReader(configFile), Config.class);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
