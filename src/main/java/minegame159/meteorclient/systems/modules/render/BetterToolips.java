/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render;

import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.misc.Keybind;
import minegame159.meteorclient.utils.render.color.SettingColor;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT;

public class BetterToolips extends Module {

    private final SettingGroup sgShulker = settings.createGroup("Shulker");
    private final SettingGroup sgEChest = settings.createGroup("EChest");
    private final SettingGroup sgMap = settings.createGroup("Map");

    // Shulker

    public final Setting<Boolean> shulkers = sgShulker.add(new BoolSetting.Builder()
            .name("shulker-preview")
            .description("Shows a preview of a shulker box when hovering over it in an inventory.")
            .defaultValue(true)
            .build()
    );

    public final Setting<DisplayWhen> shulkersDisplayWhen = sgShulker.add(new EnumSetting.Builder<DisplayWhen>()
            .name("display-when")
            .description("When to display shulker previews.")
            .defaultValue(DisplayWhen.Always)
            .onModuleActivated(setting -> validateSettings())
            .onChanged(value -> validateSettings())
            .build()
    );

    private final Setting<Keybind> shulkersKeybind = sgShulker.add(new KeybindSetting.Builder()
            .name("keybind")
            .description("The bind for keybind mode.")
            .defaultValue(Keybind.fromKey(GLFW_KEY_LEFT_ALT))
            .build()
    );

    public final Setting<DisplayMode> shulkersDisplayMode = sgShulker.add(new EnumSetting.Builder<DisplayMode>()
            .name("display-mode")
            .description("How to display shulker previews.")
            .defaultValue(DisplayMode.Container)
            .onModuleActivated(setting -> validateSettings())
            .onChanged(value -> validateSettings())
            .build()
    );

    public final Setting<SettingColor> shulkersColor = sgShulker.add(new ColorSetting.Builder()
            .name("container-color")
            .description("The color of the preview in container mode.")
            .defaultValue(new SettingColor(255, 255, 255))
            .build()
    );

    // EChest

    public final Setting<Boolean> echest = sgEChest.add(new BoolSetting.Builder()
            .name("echest-preview")
            .description("Shows a preview of your echest when hovering over it in an inventory.")
            .defaultValue(true)
            .build()
    );

    public final Setting<DisplayWhen> echestDisplayWhen = sgEChest.add(new EnumSetting.Builder<DisplayWhen>()
            .name("display-when")
            .description("When to display echest previews.")
            .defaultValue(DisplayWhen.Always)
            .onModuleActivated(setting -> validateSettings())
            .onChanged(value -> validateSettings())
            .build()
    );

    private final Setting<Keybind> echestKeybind = sgEChest.add(new KeybindSetting.Builder()
            .name("keybind")
            .description("The bind for keybind mode.")
            .defaultValue(Keybind.fromKey(GLFW_KEY_LEFT_ALT))
            .build()
    );

    public final Setting<DisplayMode> echestDisplayMode = sgEChest.add(new EnumSetting.Builder<DisplayMode>()
            .name("display-mode")
            .description("How to display echest previews.")
            .defaultValue(DisplayMode.Container)
            .onModuleActivated(setting -> validateSettings())
            .onChanged(value -> validateSettings())
            .build()
    );

    public final Setting<SettingColor> echestColor = sgEChest.add(new ColorSetting.Builder()
            .name("container-color")
            .description("The color of the preview in container mode.")
            .defaultValue(new SettingColor(255, 255, 255))
            .build()
    );

    // Map

    public final Setting<Boolean> maps = sgMap.add(new BoolSetting.Builder()
            .name("map-preview")
            .description("Shows a preview of a map when hovering over it in an inventory.")
            .defaultValue(true)
            .build()
    );

    public final Setting<DisplayWhen> mapsDisplayWhen = sgMap.add(new EnumSetting.Builder<DisplayWhen>()
            .name("display-when")
            .description("When to display map previews.")
            .defaultValue(DisplayWhen.Always)
            .build()
    );

    private final Setting<Keybind> mapsKeybind = sgMap.add(new KeybindSetting.Builder()
            .name("keybind")
            .description("The bind for keybind mode.")
            .defaultValue(Keybind.fromKey(GLFW_KEY_LEFT_ALT))
            .build()
    );

    public final Setting<Integer> mapsScale = sgMap.add(new IntSetting.Builder()
            .name("scale")
            .description("The scale of the map preview.")
            .defaultValue(1)
            .min(1)
            .sliderMax(5)
            .build()
    );

    public BetterToolips() {
        super(Categories.Render, "better-tooltips", "Displays more useful tooltips for certain items.");
    }

    public boolean previewShulkers() {
        return isActive() && shulkers.get() && ((shulkersKeybind.get().isPressed() && shulkersDisplayWhen.get() == DisplayWhen.Keybind) || shulkersDisplayWhen.get() == DisplayWhen.Always);
    }

    public boolean previewEChest() {
        return isActive() && echest.get() && ((echestKeybind.get().isPressed() && echestDisplayWhen.get() == DisplayWhen.Keybind) || echestDisplayWhen.get() == DisplayWhen.Always);
    }

    public boolean previewMaps() {
        return isActive() && maps.get() && ((mapsKeybind.get().isPressed() && mapsDisplayWhen.get() == DisplayWhen.Keybind) || mapsDisplayWhen.get() == DisplayWhen.Always);
    }

//    @EventHandler
//    private void onGetTooltip(GetTooltipEvent event) {
//
//    }

    public void validateSettings() {
        if (shulkersDisplayMode.get() == DisplayMode.Screen && shulkersDisplayWhen.get() != DisplayWhen.Keybind) shulkersDisplayWhen.set(DisplayWhen.Keybind);
        if (echestDisplayMode.get() == DisplayMode.Screen && echestDisplayWhen.get() != DisplayWhen.Keybind) echestDisplayWhen.set(DisplayWhen.Keybind);
    }

    public enum DisplayWhen {
        Keybind,
        Always
    }

    public enum DisplayMode {
        Container,
//        Tooltip,
        Screen
    }
}
