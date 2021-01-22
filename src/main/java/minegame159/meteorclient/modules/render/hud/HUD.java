/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render.hud;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.render.Render2DEvent;
import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WTable;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.combat.*;
import minegame159.meteorclient.modules.render.hud.modules.*;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.render.AlignmentX;
import minegame159.meteorclient.utils.render.AlignmentY;
import minegame159.meteorclient.utils.render.color.Color;
import minegame159.meteorclient.utils.render.color.SettingColor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public class HUD extends Module {
    private static final HudRenderer RENDERER = new HudRenderer();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgActiveModules = settings.createGroup("Active Modules");
    private final SettingGroup sgInvViewer = settings.createGroup("Inventory Viewer");
    private final SettingGroup sgPlayerModel = settings.createGroup("Player Model");
    private final SettingGroup sgArmor = settings.createGroup("Armor");
    private final SettingGroup sgModuleInfo = settings.createGroup("Module Info");
    private final SettingGroup sgCompass = settings.createGroup("Compass");


    private final ActiveModulesHud activeModulesHud = new ActiveModulesHud(this);
    private final ModuleInfoHud moduleInfoHud = new ModuleInfoHud(this);

    // General
    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale")
            .description("Scale of the HUD.")
            .defaultValue(1)
            .min(1)
            .max(3)
            .sliderMin(1)
            .sliderMax(3)
            .build()
    );

    private final Setting<SettingColor> primaryColor = sgGeneral.add(new ColorSetting.Builder()
            .name("primary-color")
            .description("Primary color of text.")
            .defaultValue(new SettingColor(255, 255, 255))
            .build()
    );

    private final Setting<SettingColor> secondaryColor = sgGeneral.add(new ColorSetting.Builder()
            .name("secondary-color")
            .description("Secondary color of text.")
            .defaultValue(new SettingColor(175, 175, 175))
            .build()
    );

    private final Setting<SettingColor> welcomeColor = sgGeneral.add(new ColorSetting.Builder()
            .name("welcome-color")
            .description("Color of welcome text.")
            .defaultValue(new SettingColor(120, 43, 153))
            .build()
    );

    // Active Modules
    private final Setting<ActiveModulesHud.Sort> activeModulesSort = sgActiveModules.add(new EnumSetting.Builder<ActiveModulesHud.Sort>()
            .name("active-modules-sort")
            .description("How to sort active modules.")
            .defaultValue(ActiveModulesHud.Sort.ByBiggest)
//            .onChanged(sort -> activeModulesHud.recalculate())
            .build()
    );

    private final Setting<Boolean> activeInfo = sgActiveModules.add(new BoolSetting.Builder()
            .name("additional-info")
            .description("Shows additional info from the module next to the name in the active modules list.")
            .defaultValue(true)
            .build()
    );

    private final Setting<ActiveModulesHud.ColorMode> activeModulesColorMode = sgActiveModules.add(new EnumSetting.Builder<ActiveModulesHud.ColorMode>()
            .name("active-modules-color-mode")
            .description("What color to use for active modules.")
            .defaultValue(ActiveModulesHud.ColorMode.Random)
            .build()
    );

    private final Setting<SettingColor> activeModulesFlatColor = sgActiveModules.add(new ColorSetting.Builder()
            .name("active-modules-flat-color")
            .description("Color for flat color mode.")
            .defaultValue(new SettingColor(225, 25, 25))
            .build()
    );

    private final Setting<Double> activeModulesRainbowSpeed = sgActiveModules.add(new DoubleSetting.Builder()
            .name("active-modules-rainbow-speed")
            .description("Rainbow speed of rainbow color mode.")
            .defaultValue(0.05)
            .sliderMax(0.1)
            .decimalPlaces(4)
            .build()
    );

    private final Setting<Double> activeModulesRainbowSpread = sgActiveModules.add(new DoubleSetting.Builder()
            .name("active-modules-rainbow-spread")
            .description("Rainbow spread of rainbow color mode.")
            .defaultValue(0.025)
            .sliderMax(0.05)
            .decimalPlaces(4)
            .build()
    );

    // Inventory Viewer
    private final Setting<InventoryViewerHud.Background> invViewerBackground = sgInvViewer.add(new EnumSetting.Builder<InventoryViewerHud.Background>()
            .name("inventory-viewer-background")
            .description("Background of inventory viewer.")
            .defaultValue(InventoryViewerHud.Background.Light)
            .build()
    );

    private final Setting<SettingColor> invViewerColor = sgInvViewer.add(new ColorSetting.Builder()
            .name("flat-mode-color")
            .description("Color of background on Flat mode.")
            .defaultValue(new SettingColor(0, 0, 0, 64))
            .build()
    );

    private final Setting<Double> invViewerScale = sgInvViewer.add(new DoubleSetting.Builder()
            .name("inventory-viewer-scale")
            .description("Scale of inventory viewer.")
            .defaultValue(2)
            .min(1)
            .max(4)
            .sliderMin(1)
            .sliderMax(4)
            .build()
    );

    // Player Model
    private final Setting<Double> playerModelScale = sgPlayerModel.add(new DoubleSetting.Builder()
            .name("player-model-scale")
            .description("Scale of player model.")
            .defaultValue(2)
            .min(1)
            .sliderMin(1)
            .sliderMax(4)
            .build()
    );

    private final Setting<Boolean> copyYaw = sgPlayerModel.add(new BoolSetting.Builder()
            .name("copy-yaw")
            .description("Makes the player model's yaw equal to yours.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> copyPitch = sgPlayerModel.add(new BoolSetting.Builder()
            .name("copy-pitch")
            .description("Makes the player model's pitch equal to yours.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> customYaw = sgPlayerModel.add(new IntSetting.Builder()
            .name("custom-yaw")
            .description("Custom yaw for when copy yaw is off.")
            .defaultValue(0)
            .min(-180)
            .max(180)
            .sliderMin(-180)
            .sliderMax(180)
            .build()
    );

    private final Setting<Integer> customPitch = sgPlayerModel.add(new IntSetting.Builder()
            .name("custom-pitch")
            .description("Custom pitch for when copy pitch is off.")
            .defaultValue(0)
            .min(-180)
            .max(180)
            .sliderMin(-180)
            .sliderMax(180)
            .build()
    );

    private final Setting<Boolean> playerModelBackground = sgPlayerModel.add(new BoolSetting.Builder()
            .name("player-model-background")
            .description("Displays a background behind the player model.")
            .defaultValue(true)
            .build()
    );

    private final Setting<SettingColor> playerModelColor = sgPlayerModel.add(new ColorSetting.Builder()
            .name("player-model-background-color")
            .description("Color of background.")
            .defaultValue(new SettingColor(0, 0, 0, 64))
            .build()
    );

    // Armor
    private final Setting<Boolean> armorFlip = sgArmor.add(new BoolSetting.Builder()
            .name("armor-flip-order")
            .description("Flips the order of armor items.")
            .defaultValue(true)
            .build()
    );

    private final Setting<ArmorHud.Orientation> armorOrientation = sgArmor.add(new EnumSetting.Builder<ArmorHud.Orientation>()
            .name("orientation")
            .description("How to display armor.")
            .defaultValue(ArmorHud.Orientation.Horizontal)
            .build()
    );


    private final Setting<ArmorHud.Durability> armorDurability = sgArmor.add(new EnumSetting.Builder<ArmorHud.Durability>()
            .name("armor-durability")
            .description("How to display armor durability.")
            .defaultValue(ArmorHud.Durability.Default)
            .build()
    );

    private final Setting<Double> armorScale = sgArmor.add(new DoubleSetting.Builder()
            .name("armor-scale")
            .description("Scale of armor.")
            .defaultValue(3.5)
            .min(2)
            .sliderMin(2)
            .sliderMax(5)
            .build()
    );

    // Module Info
    private final Setting<List<Module>> moduleInfoModules = sgModuleInfo.add(new ModuleListSetting.Builder()
            .name("module-info-modules")
            .description("Which modules to display")
            .defaultValue(moduleInfoModulesDefaultValue())
//            .onChanged(toggleModules -> moduleInfoHud.recalculate())
            .build()
    );

    private final Setting<Boolean> moduleInfo = sgModuleInfo.add(new BoolSetting.Builder()
            .name("additional-info")
            .description("Shows additional info from the module next to the name in the module info list.")
            .defaultValue(true)
            .build()
    );

    private final Setting<SettingColor> moduleInfoOnColor = sgModuleInfo.add(new ColorSetting.Builder()
            .name("module-info-on-color")
            .description("Color when module is on.")
            .defaultValue(new SettingColor(25, 225, 25))
            .build()
    );

    private final Setting<SettingColor> moduleInfoOffColor = sgModuleInfo.add(new ColorSetting.Builder()
            .name("module-info-off-color")
            .description("Color when module is off.")
            .defaultValue(new SettingColor(225, 25, 25))
            .build()
    );

    //Compass

    private final Setting<CompassHud.Mode> compassMode = sgCompass.add(new EnumSetting.Builder<CompassHud.Mode>()
            .name("inventory-viewer-background")
            .description("Background of inventory viewer.")
            .defaultValue(CompassHud.Mode.Pole)
            .build()
    );

    private final Setting<Double> compassScale = sgCompass.add(new DoubleSetting.Builder()
            .name("compass-scale")
            .description("Scale of compass.")
            .defaultValue(1)
            .sliderMin(2)
            .sliderMax(4)
            .build()
    );

    public final List<HudModule> modules = new ArrayList<>();

    public HUD() {
        super(Category.Render, "HUD", "In game overlay.");

        init();
    }

    private static List<Module> moduleInfoModulesDefaultValue() {
        List<Module> modules = new ArrayList<>();
        modules.add(ModuleManager.INSTANCE.get(KillAura.class));
        modules.add(ModuleManager.INSTANCE.get(CrystalAura.class));
        modules.add(ModuleManager.INSTANCE.get(AnchorAura.class));
        modules.add(ModuleManager.INSTANCE.get(BedAura.class));
        modules.add(ModuleManager.INSTANCE.get(Surround.class));
        return modules;
    }

    private void init() {
        modules.clear();
        RENDERER.begin(scale(), 0, true);

        // Top Left
        HudModuleLayer topLeft = new HudModuleLayer(RENDERER, modules, AlignmentX.Left, AlignmentY.Top, 2, 2);
        topLeft.add(new WatermarkHud(this));
        topLeft.add(new FpsHud(this));
        topLeft.add(new PingHud(this));
        topLeft.add(new TpsHud(this));
        topLeft.add(new SpeedHud(this));
        topLeft.add(new BiomeHud(this));
        topLeft.add(new TimeHud(this));
        topLeft.add(new DurabilityHud(this));
        topLeft.add(new BreakingBlockHud(this));
        topLeft.add(new LookingAtHud(this));
        topLeft.add(moduleInfoHud);
        topLeft.add(new InfiniteMineHud(this));

        // Top Center
        HudModuleLayer topCenter = new HudModuleLayer(RENDERER, modules, AlignmentX.Center, AlignmentY.Top, 0, 2);
        topCenter.add(new InventoryViewerHud(this));
        topCenter.add(new WelcomeHud(this));
        topCenter.add(new LagNotifierHud(this));

        // Top Right
        HudModuleLayer topRight = new HudModuleLayer(RENDERER, modules, AlignmentX.Right, AlignmentY.Top, 2, 2);
        topRight.add(activeModulesHud);

        // Bottom Left
        HudModuleLayer bottomLeft = new HudModuleLayer(RENDERER, modules, AlignmentX.Left, AlignmentY.Bottom, 2, 2);
        bottomLeft.add(new PlayerModelHud(this));

        // Bottom Center
        HudModuleLayer bottomCenter = new HudModuleLayer(RENDERER, modules, AlignmentX.Center, AlignmentY.Bottom, 48, 64);
        bottomCenter.add(new ArmorHud(this));
        bottomCenter.add(new CompassHud(this));
        bottomCenter.add(new TotemHud(this));

        // Bottom Right
        HudModuleLayer bottomRight = new HudModuleLayer(RENDERER, modules, AlignmentX.Right, AlignmentY.Bottom, 2, 2);
        bottomRight.add(new PositionHud(this));
        bottomRight.add(new RotationHud(this));
        bottomRight.add(new PotionTimersHud(this));

        RENDERER.end();
    }

    @EventHandler
    public final Listener<Render2DEvent> onRender = new Listener<>(event -> {
        if (mc.options.debugEnabled) return;

        RENDERER.begin(scale(), event.tickDelta, false);

        for (HudModule module : modules) {
            if (module.active || mc.currentScreen instanceof HudEditorScreen) {
                module.update(RENDERER);
                module.render(RENDERER);
            }
        }

        RENDERER.end();
    });

    @Override
    public WWidget getWidget() {
        WTable table = new WTable();

        WButton reset = table.add(new WButton("Reset")).getWidget();
        reset.action = this::init;
        table.add(new WLabel("Resets positions (do this after changing scale)."));
        table.row();

        WButton editor = table.add(new WButton("Editor")).getWidget();
        editor.action = () -> mc.openScreen(new HudEditorScreen());
        table.add(new WLabel("Right click elements to toggle."));

        return table;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = super.toTag();

        ListTag modulesTag = new ListTag();
        for (HudModule module : modules) modulesTag.add(module.toTag());
        tag.put("modules", modulesTag);

        return tag;
    }

    @Override
    public Module fromTag(CompoundTag tag) {
        if (tag.contains("modules")) {
            ListTag modulesTag = tag.getList("modules", 10);

            for (Tag t : modulesTag) {
                CompoundTag moduleTag = (CompoundTag) t;

                HudModule module = getModule(moduleTag.getString("name"));
                if (module != null) module.fromTag(moduleTag);
            }
        }

        return super.fromTag(tag);
    }

    private HudModule getModule(String name) {
        for (HudModule module : modules) {
            if (module.name.equals(name)) return module;
        }

        return null;
    }

    public double scale() {
        return scale.get();
    }
    public Color primaryColor() {
        return primaryColor.get();
    }
    public Color secondaryColor() {
        return secondaryColor.get();
    }
    public Color welcomeColor() {
        return welcomeColor.get();
    }

    public ActiveModulesHud.Sort activeModulesSort() {
        return activeModulesSort.get();
    }
    public boolean activeInfo() {
        return activeInfo.get();
    }
    public ActiveModulesHud.ColorMode activeModulesColorMode() {
        return activeModulesColorMode.get();
    }
    public SettingColor activeModulesFlatColor() {
        return activeModulesFlatColor.get();
    }
    public double activeModulesRainbowSpeed() {
        return activeModulesRainbowSpeed.get();
    }
    public double activeModulesRainbowSpread() {
        return activeModulesRainbowSpread.get();
    }

    public InventoryViewerHud.Background invViewerBackground() {
        return invViewerBackground.get();
    }
    public Color invViewerColor() {
        return invViewerColor.get();
    }
    public double invViewerScale() {
        return invViewerScale.get();
    }

    public double playerModelScale() {
        return playerModelScale.get();
    }
    public boolean playerModelCopyYaw() {
        return copyYaw.get();
    }
    public boolean playerModelCopyPitch() {
        return copyPitch.get();
    }
    public int playerModelCustomYaw() {
        return customYaw.get();
    }
    public int playerModelCustomPitch() {
        return customPitch.get();
    }
    public boolean playerModelBackground() {
        return playerModelBackground.get();
    }
    public Color playerModelColor() {
        return playerModelColor.get();
    }


    public boolean armorFlip() {
        return armorFlip.get();
    }
    public ArmorHud.Orientation armorOrientation() {
        return armorOrientation.get();
    }
    public ArmorHud.Durability armorDurability() {
        return armorDurability.get();
    }
    public double armorScale() {
        return armorScale.get();
    }

    public List<Module> moduleInfoModules() {
        return moduleInfoModules.get();
    }
    public boolean moduleInfo() {
        return moduleInfo.get();
    }
    public Color moduleInfoOnColor() {
        return moduleInfoOnColor.get();
    }
    public Color moduleInfoOffColor() {
        return moduleInfoOffColor.get();
    }

    public double compassScale() {
        return compassScale.get();
    }
    public CompassHud.Mode compassMode() {
        return compassMode.get();
    }
}
