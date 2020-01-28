package minegame159.meteorclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import minegame159.jes.SubscribeEvent;
import minegame159.jes.eventbuses.DefaultEventBus;
import minegame159.jes.eventbuses.EventBus;
import minegame159.meteorclient.clickgui.ClickGUI;
import minegame159.meteorclient.commands.CommandManager;
import minegame159.meteorclient.events.TickEvent;
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

public class MeteorClient implements ClientModInitializer {
    public static MeteorClient instance;
    public static EventBus eventBus = new DefaultEventBus();
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

        CommandManager.init();
        ModuleManager.init();

        KeyBindingRegistry.INSTANCE.register(openClickGui);

        eventBus.register(this);
    }

    @SubscribeEvent
    private void onTick(TickEvent e) {
        if (openClickGui.isPressed() && !mc.isPaused()) {
            mc.openScreen(new ClickGUI());

            /*WidgetScreen screen = new WidgetScreen("Test");

            WindowCenter root = new WindowCenter(0);
            Background bg = new Background(4);
            VerticalContainer vContainer = new VerticalContainer(0, 4);

            // Title
            Container titleContainer = new Container(0, true, false);
            titleContainer.addWidget(new Label(0, "Title!", true));
            vContainer.addWidget(titleContainer);

            vContainer.addWidget(new Separator(0));

            // Label1
            Label label1 = new Label(0, "Nightmare, nightmare, nightmare!");
            label1.tooltip = "Test tooltip so enjoy.";
            vContainer.addWidget(label1);

            // Button1
            Container containerBtn1 = new Container(0, true, false);
            containerBtn1.addWidget(new Button(3, "Click Me!", button -> System.out.println("You clicked!")));
            vContainer.addWidget(containerBtn1);

            // Label2
            Label label2 = new Label(0, "This is the best client ever made.");
            label2.tooltip = "And that's a fact!";
            vContainer.addWidget(label2);

            vContainer.addWidget(new Separator(0));

            // Right label
            Container rightContainer = new Container(0, false, true);
            rightContainer.addWidget(new Label(0, "Right"));
            vContainer.addWidget(rightContainer);

            vContainer.addWidget(new Separator(0));

            // Checkboxes
            HorizontalContainer checkboxes = new HorizontalContainer(0, 4);
            checkboxes.addWidget(new Label(0, "Something: "));
            checkboxes.addWidget(new Checkbox(0, null));
            checkboxes.addWidget(new Checkbox(0, true, null));
            checkboxes.addWidget(new Checkbox(0, null));
            checkboxes.addWidget(new Checkbox(0, true, null));
            vContainer.addWidget(checkboxes);

            vContainer.addWidget(new Separator(0));

            // Grid
            Grid grid = new Grid(0, 3, 4, 4);
            //     Players
            grid.addRow(
                    new Label(0, "Players: "),
                    new Checkbox(0, true, null),
                    new Button(3, "Reset", null)
            );
            //     Animals
            grid.addRow(
                    new Label(0, "Animals: "),
                    new Checkbox(0, null),
                    new Button(3, "Reset", null)
            );
            //     Mobs
            grid.addRow(
                    new Label(0, "Mobs: "),
                    new Checkbox(0, null),
                    new Button(3, "Reset", null)
            );
            //     Items
            grid.addRow(
                    new Label(0, "Items: "),
                    new Checkbox(0, true, null),
                    new Button(3, "Reset", null)
            );
            vContainer.addWidget(grid);

            vContainer.addWidget(new Separator(0));

            // Name
            HorizontalContainer nameContainer = new HorizontalContainer(0, 4);
            nameContainer.addWidget(new Label(0, "Name: "));
            nameContainer.addWidget(new TextBox(3, "MineGame159", 16, null, null));
            vContainer.addWidget(nameContainer);

            bg.addWidget(vContainer);
            root.addWidget(bg);
            screen.addWidget(root);

            mc.openScreen(screen);*/
        }
    }

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
            try {
                Config.instance = gson.fromJson(new FileReader(configFile), Config.class);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
