/*
 *
 *  * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 *  * Copyright (c) 2021 Meteor Development.
 *
 */

package minegame159.meteorclient.modules.render.hud;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.render.Render2DEvent;
import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WTable;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.modules.combat.*;
import minegame159.meteorclient.modules.render.hud.modules.*;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.render.AlignmentX;
import minegame159.meteorclient.utils.render.AlignmentY;
import minegame159.meteorclient.utils.render.color.SettingColor;
import net.minecraft.enchantment.Enchantment;
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
    private final SettingGroup sgCombatInfo = settings.createGroup("Combat Info");
    private final SettingGroup sgArmor = settings.createGroup("Armor Info");
    private final SettingGroup sgModuleInfo = settings.createGroup("Module Info");
    private final SettingGroup sgCompass = settings.createGroup("Compass");
    private final SettingGroup sgTotemCount = settings.createGroup("Totem Count");

    private final ActiveModulesHud activeModulesHud = new ActiveModulesHud(this);
    private final ModuleInfoHud moduleInfoHud = new ModuleInfoHud(this);

    // General

    public final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale")
            .description("Scale of the HUD.")
            .defaultValue(1)
            .min(1)
            .max(3)
            .sliderMin(1)
            .sliderMax(3)
            .build()
    );

    public final Setting<SettingColor> primaryColor = sgGeneral.add(new ColorSetting.Builder()
            .name("primary-color")
            .description("Primary color of text.")
            .defaultValue(new SettingColor(255, 255, 255))
            .build()
    );

    public final Setting<SettingColor> secondaryColor = sgGeneral.add(new ColorSetting.Builder()
            .name("secondary-color")
            .description("Secondary color of text.")
            .defaultValue(new SettingColor(175, 175, 175))
            .build()
    );

    public final Setting<SettingColor> welcomeColor = sgGeneral.add(new ColorSetting.Builder()
            .name("welcome-color")
            .description("Color of welcome text.")
            .defaultValue(new SettingColor(120, 43, 153))
            .build()
    );

    // Active Modules

    public final Setting<ActiveModulesHud.Sort> activeModulesSort = sgActiveModules.add(new EnumSetting.Builder<ActiveModulesHud.Sort>()
            .name("active-modules-sort")
            .description("How to sort active modules.")
            .defaultValue(ActiveModulesHud.Sort.Biggest)
//            .onChanged(sort -> activeModulesHud.recalculate())
            .build()
    );

    public final Setting<Boolean> activeInfo = sgActiveModules.add(new BoolSetting.Builder()
            .name("additional-info")
            .description("Shows additional info from the module next to the name in the active modules list.")
            .defaultValue(true)
            .build()
    );

    public final Setting<ActiveModulesHud.ColorMode> activeModulesColorMode = sgActiveModules.add(new EnumSetting.Builder<ActiveModulesHud.ColorMode>()
            .name("active-modules-color-mode")
            .description("What color to use for active modules.")
            .defaultValue(ActiveModulesHud.ColorMode.Rainbow)
            .build()
    );

    public final Setting<SettingColor> activeModulesFlatColor = sgActiveModules.add(new ColorSetting.Builder()
            .name("active-modules-flat-color")
            .description("Color for flat color mode.")
            .defaultValue(new SettingColor(225, 25, 25))
            .build()
    );

    public final Setting<Double> activeModulesRainbowSpeed = sgActiveModules.add(new DoubleSetting.Builder()
            .name("active-modules-rainbow-speed")
            .description("Rainbow speed of rainbow color mode.")
            .defaultValue(0.05)
            .sliderMax(0.1)
            .decimalPlaces(4)
            .build()
    );

    public final Setting<Double> activeModulesRainbowSpread = sgActiveModules.add(new DoubleSetting.Builder()
            .name("active-modules-rainbow-spread")
            .description("Rainbow spread of rainbow color mode.")
            .defaultValue(0.025)
            .sliderMax(0.05)
            .decimalPlaces(4)
            .build()
    );

    // Inventory Viewer

    public final Setting<InventoryViewerHud.Background> invViewerBackground = sgInvViewer.add(new EnumSetting.Builder<InventoryViewerHud.Background>()
            .name("inventory-viewer-background")
            .description("Background of inventory viewer.")
            .defaultValue(InventoryViewerHud.Background.Light)
            .build()
    );

    public final Setting<SettingColor> invViewerColor = sgInvViewer.add(new ColorSetting.Builder()
            .name("flat-mode-color")
            .description("Color of background on Flat mode.")
            .defaultValue(new SettingColor(0, 0, 0, 64))
            .build()
    );

    public final Setting<Double> invViewerScale = sgInvViewer.add(new DoubleSetting.Builder()
            .name("inventory-viewer-scale")
            .description("Scale of inventory viewer.")
            .defaultValue(2)
            .min(1)
            .max(4)
            .sliderMin(1)
            .sliderMax(4)
            .build()
    );

    // Armor Info

    public final Setting<Boolean> armorInfoFlip = sgArmor.add(new BoolSetting.Builder()
            .name("armor-flip-order")
            .description("Flips the order of armor items.")
            .defaultValue(true)
            .build()
    );

    public final Setting<ArmorHud.Orientation> armorInfoOrientation = sgArmor.add(new EnumSetting.Builder<ArmorHud.Orientation>()
            .name("orientation")
            .description("How to display armor.")
            .defaultValue(ArmorHud.Orientation.Horizontal)
            .build()
    );

    public final Setting<ArmorHud.Durability> armorInfoDurability = sgArmor.add(new EnumSetting.Builder<ArmorHud.Durability>()
            .name("armor-durability")
            .description("How to display armor durability.")
            .defaultValue(ArmorHud.Durability.Default)
            .build()
    );

    public final Setting<Double> armorInfoScale = sgArmor.add(new DoubleSetting.Builder()
            .name("armor-scale")
            .description("Scale of armor.")
            .defaultValue(3.5)
            .min(2)
            .sliderMin(2)
            .sliderMax(5)
            .build()
    );

    // Compass

    public final Setting<CompassHud.Mode> compassMode = sgCompass.add(new EnumSetting.Builder<CompassHud.Mode>()
            .name("mode")
            .description("The mode of the compass.")
            .defaultValue(CompassHud.Mode.Pole)
            .build()
    );

    public final Setting<Double> compassScale = sgCompass.add(new DoubleSetting.Builder()
            .name("scale")
            .description("The scale of compass.")
            .defaultValue(2.5)
            .sliderMin(1)
            .sliderMax(5)
            .build()
    );

    //Totem

    public final Setting<Double> totemCountScale = sgTotemCount.add(new DoubleSetting.Builder()
            .name("scale")
            .description("Scale of totem counter.")
            .defaultValue(2)
            .min(1)
            .sliderMin(1)
            .sliderMax(4)
            .build()
    );

    // Module Info

    public final Setting<List<Module>> moduleInfoModules = sgModuleInfo.add(new ModuleListSetting.Builder()
            .name("module-info-modules")
            .description("Which modules to display")
            .defaultValue(moduleInfoModulesDefaultValue())
            .build()
    );

    public final Setting<Boolean> moduleInfo = sgModuleInfo.add(new BoolSetting.Builder()
            .name("additional-info")
            .description("Shows additional info from the module next to the name in the module info list.")
            .defaultValue(true)
            .build()
    );

    public final Setting<SettingColor> moduleInfoOnColor = sgModuleInfo.add(new ColorSetting.Builder()
            .name("module-info-on-color")
            .description("Color when module is on.")
            .defaultValue(new SettingColor(25, 225, 25))
            .build()
    );

    public final Setting<SettingColor> moduleInfoOffColor = sgModuleInfo.add(new ColorSetting.Builder()
            .name("module-info-off-color")
            .description("Color when module is off.")
            .defaultValue(new SettingColor(225, 25, 25))
            .build()
    );

    // Player Model
    public final Setting<Double> playerModelScale = sgPlayerModel.add(new DoubleSetting.Builder()
            .name("player-model-scale")
            .description("Scale of player model.")
            .defaultValue(2)
            .min(1)
            .sliderMin(1)
            .sliderMax(4)
            .build()
    );

    public final Setting<Boolean> playerModelCopyYaw = sgPlayerModel.add(new BoolSetting.Builder()
            .name("copy-yaw")
            .description("Makes the player model's yaw equal to yours.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Boolean> playerModelCopyPitch = sgPlayerModel.add(new BoolSetting.Builder()
            .name("copy-pitch")
            .description("Makes the player model's pitch equal to yours.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Integer> playerModelCustomYaw = sgPlayerModel.add(new IntSetting.Builder()
            .name("custom-yaw")
            .description("Custom yaw for when copy yaw is off.")
            .defaultValue(0)
            .min(-180)
            .max(180)
            .sliderMin(-180)
            .sliderMax(180)
            .build()
    );

    public final Setting<Integer> playerModelCustomPitch = sgPlayerModel.add(new IntSetting.Builder()
            .name("custom-pitch")
            .description("Custom pitch for when copy pitch is off.")
            .defaultValue(0)
            .min(-180)
            .max(180)
            .sliderMin(-180)
            .sliderMax(180)
            .build()
    );

    public final Setting<Boolean> playerModelBackground = sgPlayerModel.add(new BoolSetting.Builder()
            .name("player-model-background")
            .description("Displays a background behind the player model.")
            .defaultValue(true)
            .build()
    );

    public final Setting<SettingColor> playerModelBackgroundColor = sgPlayerModel.add(new ColorSetting.Builder()
            .name("background-color")
            .description("Color of background.")
            .defaultValue(new SettingColor(0, 0, 0, 64))
            .build()
    );

    //Combat info
    public final Setting<Double> combatInfoScale = sgCombatInfo.add(new DoubleSetting.Builder()
            .name("scale")
            .description("Scale of combat info.")
            .defaultValue(2)
            .min(1)
            .sliderMin(1)
            .sliderMax(4)
            .build()
    );

    public final Setting<Double> combatInfoRange = sgCombatInfo.add(new DoubleSetting.Builder()
            .name("range")
            .description("The range to target players.")
            .defaultValue(100)
            .min(1)
            .sliderMax(200)
            .build()
    );

    public final Setting<Boolean> combatInfoIgnoreFriends = sgCombatInfo.add(new BoolSetting.Builder()
            .name("ignore-friends")
            .description("Ignores friends when targeting.")
            .defaultValue(false)
            .build()
    );

    public final Setting<Boolean> combatInfoDisplayPing = sgCombatInfo.add(new BoolSetting.Builder()
            .name("ping")
            .description("Shows the player's ping.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Boolean> combatInfoDisplayDist = sgCombatInfo.add(new BoolSetting.Builder()
            .name("distance")
            .description("Shows the distance between you and the player.")
            .defaultValue(true)
            .build()
    );

    public final Setting<List<Enchantment>> combatInfoDisplayedEnchantments = sgCombatInfo.add(new EnchListSetting.Builder()
            .name("displayed-enchantments")
            .description("The enchantments that are shown on nametags.")
            .defaultValue(CombatHud.setDefualtList())
            .build()
    );

    public final Setting<SettingColor> combatInfoBackgroundColor = sgCombatInfo.add(new ColorSetting.Builder()
            .name("background-color")
            .description("Color of background.")
            .defaultValue(new SettingColor(0, 0, 0, 64))
            .build()
    );

    public final Setting<SettingColor> combatInfoEnchantmentTextColor = sgCombatInfo.add(new ColorSetting.Builder()
            .name("enchantment-color")
            .description("Color of enchantment text.")
            .defaultValue(new SettingColor(255, 255, 255))
            .build()
    );

    public final Setting<SettingColor> combatInfoPingColor1 = sgCombatInfo.add(new ColorSetting.Builder()
            .name("ping-stage-1")
            .description("Color of ping text when under 75.")
            .defaultValue(new SettingColor(15, 255, 15))
            .build()
    );

    public final Setting<SettingColor> combatInfoPingColor2 = sgCombatInfo.add(new ColorSetting.Builder()
            .name("ping-stage-2")
            .description("Color of ping text when between 75 and 200.")
            .defaultValue(new SettingColor(255, 150, 15))
            .build()
    );

    public final Setting<SettingColor> combatInfoPingColor3 = sgCombatInfo.add(new ColorSetting.Builder()
            .name("ping-stage-3")
            .description("Color of ping text when over 200.")
            .defaultValue(new SettingColor(255, 15, 15))
            .build()
    );

    public final Setting<SettingColor> combatInfoDistColor1 = sgCombatInfo.add(new ColorSetting.Builder()
            .name("distance-stage-1")
            .description("The color when a player is within 10 blocks of you.")
            .defaultValue(new SettingColor(255, 15, 15))
            .build()
    );

    public final Setting<SettingColor> combatInfoDistColor2 = sgCombatInfo.add(new ColorSetting.Builder()
            .name("distance-stage-2")
            .description("The color when a player is within 50 blocks of you.")
            .defaultValue(new SettingColor(255, 150, 15))
            .build()
    );

    public final Setting<SettingColor> combatInfoDistColor3 = sgCombatInfo.add(new ColorSetting.Builder()
            .name("distance-stage-3")
            .description("The color when a player is greater then 50 blocks away from you.")
            .defaultValue(new SettingColor(15, 255, 15))
            .build()
    );

    public final Setting<SettingColor> combatInfoHealthColor1 = sgCombatInfo.add(new ColorSetting.Builder()
            .name("healh-stage-1")
            .description("The color on the left of the health gradient.")
            .defaultValue(new SettingColor(255, 15, 15))
            .build()
    );

    public final Setting<SettingColor> combatInfoHealthColor2 = sgCombatInfo.add(new ColorSetting.Builder()
            .name("health-stage-2")
            .description("The color in the middle of the health gradient.")
            .defaultValue(new SettingColor(255, 150, 15))
            .build()
    );

    public final Setting<SettingColor> combatInfoHealthColor3 = sgCombatInfo.add(new ColorSetting.Builder()
            .name("health-stage-3")
            .description("The color on the right of the health gradient.")
            .defaultValue(new SettingColor(15, 255, 15))
            .build()
    );

    public final List<HudModule> modules = new ArrayList<>();

    public HUD() {
        super(Category.Render, "HUD", "In game overlay.");

        init();
    }

    private static List<Module> moduleInfoModulesDefaultValue() {
        List<Module> modules = new ArrayList<>();
        modules.add(Modules.get().get(KillAura.class));
        modules.add(Modules.get().get(CrystalAura.class));
        modules.add(Modules.get().get(AnchorAura.class));
        modules.add(Modules.get().get(BedAura.class));
        modules.add(Modules.get().get(Surround.class));
        return modules;
    }

    private void init() {
        modules.clear();
        RENDERER.begin(scale.get(), 0, true);

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
        bottomRight.add(new CombatHud(this));

        RENDERER.end();
    }

    @EventHandler
    public void onRender(Render2DEvent event) {
        if (mc.options.debugEnabled) return;

        RENDERER.begin(scale.get(), event.tickDelta, false);

        for (HudModule module : modules) {
            if (module.active || mc.currentScreen instanceof HudEditorScreen) {
                module.update(RENDERER);
                module.render(RENDERER);
            }
        }

        RENDERER.end();
    }

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
}
