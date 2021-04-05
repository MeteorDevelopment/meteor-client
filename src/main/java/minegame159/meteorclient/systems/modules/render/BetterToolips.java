/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render;

import minegame159.meteorclient.events.game.GetTooltipEvent;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.utils.misc.Keybind;
import minegame159.meteorclient.utils.misc.ByteCountDataOutput;
import minegame159.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;

import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.LiteralText;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;

public class BetterToolips extends Module {

    private final SettingGroup sgShulker = settings.createGroup("Shulker");
    private final SettingGroup sgEChest = settings.createGroup("EChest");
    private final SettingGroup sgMap = settings.createGroup("Map");
    private final SettingGroup sgByteSize = settings.createGroup("Byte Size");


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

    private final Setting<SettingColor> shulkersColor = sgShulker.add(new ColorSetting.Builder()
            .name("container-color")
            .description("The color of the preview in container mode.")
            .defaultValue(new SettingColor(255, 255, 255))
            .build()
    );

    private final Setting<Boolean> shulkerColorFromType = sgShulker.add(new BoolSetting.Builder()
        .name("color-from-type")
        .description("Color shulker preview according to its color.")
        .defaultValue(false)
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

    // Byte Size

    public final Setting<Boolean> byteSize = sgByteSize.add(new BoolSetting.Builder()
            .name("byte-size")
            .description("Displays an item's size in bytes in the tooltip.")
            .defaultValue(true)
            .build()
    );

    
    public final Setting<DisplayWhen> byteSizeDisplayWhen = sgByteSize.add(new EnumSetting.Builder<DisplayWhen>()
            .name("display-when")
            .description("When to display byte size.")
            .defaultValue(DisplayWhen.Always)
            .build()
    );

    private final Setting<Keybind> byteSizeKeybind = sgByteSize.add(new KeybindSetting.Builder()
            .name("keybind")
            .description("The bind for keybind mode.")
            .defaultValue(Keybind.fromKey(GLFW_KEY_LEFT_SHIFT))
            .build()
    );

    private final Setting<Boolean> useKbIfBigEnoughEnabled = sgByteSize.add(new BoolSetting.Builder()
            .name("use-kb-if-big-enough-enabled")
            .description("Uses KB instead of bytes if your item's size is larger or equal to 1KB.")
            .defaultValue(true)
            .build()
    );

    private final Setting<ByteDisplayMode> byteDisplayMode = sgByteSize.add(new EnumSetting.Builder<ByteDisplayMode>()
            .name("byte-display-mode")
            .description("Uses the standard mode (1KB to 1000b) OR true mode (1KB to 1024b).")
            .defaultValue(ByteDisplayMode.True)
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

    private boolean displayByteSize() {
        return isActive() && byteSize.get() && ((byteSizeKeybind.get().isPressed() && byteSizeDisplayWhen.get() == DisplayWhen.Keybind) || byteSizeDisplayWhen.get() == DisplayWhen.Always);
    }

    public SettingColor getShulkerColor(ItemStack shulkerItem) {
        if (shulkerColorFromType.get()) {
            if (!(shulkerItem.getItem() instanceof BlockItem)) return shulkersColor.get();
            Block block = ((BlockItem) shulkerItem.getItem()).getBlock();
            if (!(block instanceof ShulkerBoxBlock)) return shulkersColor.get();
            ShulkerBoxBlock shulkerBlock = (ShulkerBoxBlock) ShulkerBoxBlock.getBlockFromItem(shulkerItem.getItem());
            DyeColor dye = shulkerBlock.getColor();
            if (dye == null) return shulkersColor.get();
            final float[] colors = dye.getColorComponents();
            return new SettingColor(colors[0], colors[1], colors[2], 1f);
        } else {
            return shulkersColor.get();
        }
    }

    @EventHandler
    private void onGetTooltip(GetTooltipEvent event) {
        if (displayByteSize()) {
            try {
                event.itemStack.toTag(new CompoundTag()).write(ByteCountDataOutput.INSTANCE);
                int byteCount = ByteCountDataOutput.INSTANCE.getCount();
                ByteCountDataOutput.INSTANCE.reset();
    
                event.list.add(new LiteralText(Formatting.GRAY + Modules.get().get(BetterToolips.class).bytesToString(byteCount)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
    }

    public void validateSettings() {
        if (shulkersDisplayMode.get() == DisplayMode.Screen && shulkersDisplayWhen.get() != DisplayWhen.Keybind) shulkersDisplayWhen.set(DisplayWhen.Keybind);
        if (echestDisplayMode.get() == DisplayMode.Screen && echestDisplayWhen.get() != DisplayWhen.Keybind) echestDisplayWhen.set(DisplayWhen.Keybind);
    }

    private int getKbSize() {
        return byteDisplayMode.get() == ByteDisplayMode.True ? 1024 : 1000;
    }

    public String bytesToString(int count) {
        if (useKbIfBigEnoughEnabled.get() && count >= getKbSize()) return String.format("%.2f kb", count / (float) getKbSize());
        return String.format("%d bytes", count);
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

    public enum ByteDisplayMode {
        Standard,
        True
    }
}
