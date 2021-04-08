/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.game.GetTooltipEvent;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.misc.ByteCountDataOutput;
import minegame159.meteorclient.utils.misc.Keybind;
import minegame159.meteorclient.utils.render.color.Color;
import minegame159.meteorclient.utils.render.color.SettingColor;
import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.item.BlockItem;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;

import java.io.IOException;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT;

public class BetterTooltips extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgShulker = settings.createGroup("Shulker");
    private final SettingGroup sgEChest = settings.createGroup("EChest");
    private final SettingGroup sgMap = settings.createGroup("Map");
    private final SettingGroup sgOther = settings.createGroup("Other");

    // General

    private final Setting<DisplayWhen> displayWhen = sgGeneral.add(new EnumSetting.Builder<DisplayWhen>()
            .name("display-when")
            .description("When to display previews.")
            .defaultValue(DisplayWhen.Keybind)
            .build()
    );

    private final Setting<Keybind> keybind = sgGeneral.add(new KeybindSetting.Builder()
            .name("keybind")
            .description("The bind for keybind mode.")
            .defaultValue(Keybind.fromKey(GLFW_KEY_LEFT_ALT))
            .build()
    );

    public final Setting<Boolean> showVanilla = sgGeneral.add(new BoolSetting.Builder()
            .name("show-vanilla")
            .description("Displays the vanilla tooltip as well as the preview.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Boolean> middleClickOpen = sgGeneral.add(new BoolSetting.Builder()
            .name("middle-click-open")
            .description("Opens a GUI window with the inventory of the storage block when you middle click the item.")
            .defaultValue(true)
            .build()
    );

    // Shulker

    private final Setting<Boolean> shulkers = sgShulker.add(new BoolSetting.Builder()
            .name("shulker-preview")
            .description("Shows a preview of a shulker box when hovering over it in an inventory.")
            .defaultValue(true)
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
        .description("Color shulker preview according to the shulkers color.")
        .defaultValue(true)
        .build()
    );

    // EChest

    public final Setting<Boolean> echest = sgEChest.add(new BoolSetting.Builder()
            .name("echest-preview")
            .description("Shows a preview of your echest when hovering over it in an inventory.")
            .defaultValue(true)
            .build()
    );

    public final Setting<SettingColor> echestColor = sgEChest.add(new ColorSetting.Builder()
            .name("container-color")
            .description("The color of the echest preview in container mode.")
            .defaultValue(new SettingColor(0, 50, 50))
            .build()
    );

    // Map

    private final Setting<Boolean> maps = sgMap.add(new BoolSetting.Builder()
            .name("map-preview")
            .description("Shows a preview of a map when hovering over it in an inventory.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Integer> mapsScale = sgMap.add(new IntSetting.Builder()
            .name("scale")
            .description("The scale of the map preview.")
            .defaultValue(100)
            .min(1)
            .sliderMax(500)
            .build()
    );

    // Byte Size

    public final Setting<Boolean> byteSize = sgOther.add(new BoolSetting.Builder()
            .name("byte-size")
            .description("Displays an item's size in bytes in the tooltip.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> statusEffects = sgOther.add(new BoolSetting.Builder()
            .name("status-effects")
            .description("Adds list of status effects to tooltips of food items.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> beehive = sgOther.add(new BoolSetting.Builder()
        .name("beehive")
        .description("Displays information about a beehive or bee nest.")
        .defaultValue(true)
        .build()
    );

    public BetterTooltips() {
        super(Categories.Render, "better-tooltips", "Displays more useful tooltips for certain items.");
    }

    public boolean previewShulkers() {
        return isActive() && isPressed() && shulkers.get();
    }

    public boolean previewEChest() {
        return isActive() && isPressed() && echest.get();
    }

    public boolean previewMaps() {
        return isActive() && isPressed() && maps.get();
    }

    private boolean isPressed() {
        return (keybind.get().isPressed() && displayWhen.get() == DisplayWhen.Keybind) || displayWhen.get() == DisplayWhen.Always;
    }

    public Color getShulkerColor(ItemStack shulkerItem) {
        if (shulkerColorFromType.get()) {
            if (!(shulkerItem.getItem() instanceof BlockItem)) return shulkersColor.get();
            Block block = ((BlockItem) shulkerItem.getItem()).getBlock();
            if (!(block instanceof ShulkerBoxBlock)) return shulkersColor.get();
            ShulkerBoxBlock shulkerBlock = (ShulkerBoxBlock) ShulkerBoxBlock.getBlockFromItem(shulkerItem.getItem());
            DyeColor dye = shulkerBlock.getColor();
            if (dye == null) return shulkersColor.get();
            final float[] colors = dye.getColorComponents();
            return new Color(colors[0], colors[1], colors[2], 1f);
        } else {
            return shulkersColor.get();
        }
    }

    public static boolean hasItems(ItemStack itemStack) {
        CompoundTag compoundTag = itemStack.getSubTag("BlockEntityTag");
        return compoundTag != null && compoundTag.contains("Items", 9);
    }

    @EventHandler
    private void appendTooltip(GetTooltipEvent.Append event) { 
        // Item size tooltip
        if (byteSize.get()) {
            try {
                event.itemStack.toTag(new CompoundTag()).write(ByteCountDataOutput.INSTANCE);

                int byteCount = ByteCountDataOutput.INSTANCE.getCount();
                String count;

                ByteCountDataOutput.INSTANCE.reset();

                if (byteCount >= 1024) count = String.format("%.2f kb", byteCount / (float) 1024);
                else count = String.format("%d bytes", byteCount);
    
                event.list.add(new LiteralText(Formatting.GRAY + count));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Status effects
        if (statusEffects.get()) {
            if (event.itemStack.getItem() == Items.SUSPICIOUS_STEW) {
                CompoundTag tag = event.itemStack.getTag();
                if (tag != null) {
                    ListTag effects = tag.getList("Effects", 10);
                    if (effects != null) {
                        for (int i = 0; i < effects.size(); i++) {
                            CompoundTag effectTag = effects.getCompound(i);
                            byte effectId = effectTag.getByte("EffectId");
                            int effectDuration = effectTag.contains("EffectDuration") ? effectTag.getInt("EffectDuration") : 160;
                            StatusEffectInstance effect = new StatusEffectInstance(StatusEffect.byRawId(effectId), effectDuration, 0);
                            event.list.add(1, getStatusText(effect));
                        }
                    }
                }
            }
            else if (event.itemStack.getItem().isFood()) {
                FoodComponent food = event.itemStack.getItem().getFoodComponent();
                if (food != null) {
                    food.getStatusEffects().forEach((e) -> {
                        StatusEffectInstance effect = e.getFirst();
                        event.list.add(1, getStatusText(effect));
                    });
                }
            }
        }

        //Beehive
        if (beehive.get()) {
            if (event.itemStack.getItem() == Items.BEEHIVE || event.itemStack.getItem() == Items.BEE_NEST) {
                CompoundTag tag = event.itemStack.getTag();
                if (tag != null) {
                    CompoundTag blockStateTag = tag.getCompound("BlockStateTag");
                    if (blockStateTag != null) {
                        int level = blockStateTag.getInt("honey_level");
                        event.list.add(1, new LiteralText(String.format("%sHoney level: %s%d%s.", 
                            Formatting.GRAY, Formatting.YELLOW, level, Formatting.GRAY)));
                    }
                    CompoundTag blockEntityTag = tag.getCompound("BlockEntityTag");
                    if (blockEntityTag != null) {
                        ListTag beesTag = blockEntityTag.getList("Bees", 10);
                        event.list.add(1, new LiteralText(String.format("%sBees: %s%d%s.", 
                            Formatting.GRAY, Formatting.YELLOW, beesTag.size(), Formatting.GRAY)));
                    }
                    
                }
            }
        }

        // Hold to preview tooltip
        if (hasItems(event.itemStack) && shulkers.get() && !previewShulkers()
            || (event.itemStack.getItem() == Items.ENDER_CHEST && echest.get() && !previewEChest())
            || (event.itemStack.getItem() == Items.FILLED_MAP && maps.get() && !previewMaps())) {
            event.list.add(new LiteralText(""));
            event.list.add(new LiteralText("Hold " + Formatting.YELLOW + keybind + Formatting.RESET + " to preview"));
        }
    }

    @EventHandler
    private void modifyTooltip(GetTooltipEvent.Modify event) {
        // Moving vanilla tooltip up when container is rendered
        if (hasItems(event.itemStack) && shulkers.get() && previewShulkers() || (event.itemStack.getItem() == Items.ENDER_CHEST && echest.get() && previewEChest())) {
            for (int s = 0; s < event.list.size(); ++s) event.y -= 10;
            event.y -= 4;
        }
    }

    private MutableText getStatusText(StatusEffectInstance effect) {
        MutableText text = new TranslatableText(effect.getTranslationKey());
        if (effect.getAmplifier() != 0) {
            text.append(String.format(" %d (%s)", effect.getAmplifier()+1, StatusEffectUtil.durationToString(effect, 1)));
        } else {
            text.append(String.format(" (%s)", StatusEffectUtil.durationToString(effect, 1)));
        }
        if (effect.getEffectType().isBeneficial()) {
            return text.formatted(Formatting.BLUE);
        } else {
            return text.formatted(Formatting.RED);
        }
    }

    public enum DisplayWhen {
        Keybind,
        Always
    }
}
