/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import meteordevelopment.meteorclient.events.game.GetTooltipEvent;
import meteordevelopment.meteorclient.events.render.TooltipDataEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.ByteCountDataOutput;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.EChestMemory;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.tooltip.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT;

public class BetterTooltips extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPreviews = settings.createGroup("Previews");
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
            .visible(() -> displayWhen.get() == DisplayWhen.Keybind)
            .build()
    );

    public final Setting<Boolean> showVanilla = sgGeneral.add(new BoolSetting.Builder()
            .name("show-vanilla")
            .description("Displays the vanilla tooltip as well as the preview.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> middleClickOpen = sgGeneral.add(new BoolSetting.Builder()
            .name("middle-click-open")
            .description("Opens a GUI window with the inventory of the storage block when you middle click the item.")
            .defaultValue(true)
            .build()
    );

    // Previews

    private final Setting<Boolean> shulkers = sgPreviews.add(new BoolSetting.Builder()
            .name("storage-blocks")
            .description("Shows a preview of a shulker box when hovering over it in an inventory.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> shulkerCompactTooltip = sgPreviews.add(new BoolSetting.Builder()
            .name("compact-shulker-tooltip")
            .description("Compacts the lines of the shulker tooltip.")
            .defaultValue(true)
            .visible(shulkers::get)
            .build()
    );

    public final Setting<Boolean> echest = sgPreviews.add(new BoolSetting.Builder()
            .name("echests")
            .description("Shows a preview of your echest when hovering over it in an inventory.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> maps = sgPreviews.add(new BoolSetting.Builder()
            .name("maps")
            .description("Shows a preview of a map when hovering over it in an inventory.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Double> mapsScale = sgPreviews.add(new DoubleSetting.Builder()
            .name("map-scale")
            .description("The scale of the map preview.")
            .defaultValue(1)
            .min(0.001)
            .sliderMax(1)
            .visible(maps::get)
            .build()
    );

    private final Setting<Boolean> books = sgPreviews.add(new BoolSetting.Builder()
            .name("books")
            .description("Shows contents of a book when hovering over it in an inventory.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> banners = sgPreviews.add(new BoolSetting.Builder()
        .name("banners")
        .description("Shows banners' patterns when hovering over it in an inventory.")
        .defaultValue(true)
        .build()
    );

    // Extras

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

    public static final Color ECHEST_COLOR = new Color(0, 50, 50);

    public BetterTooltips() {
        super(Categories.Render, "better-tooltips", "Displays more useful tooltips for certain items.");
    }

    public boolean previewShulkers() {
        return isActive() && isPressed() && shulkers.get();
    }

    public boolean shulkerCompactTooltip() {
        return isActive() && shulkerCompactTooltip.get();
    }

    boolean previewEChest() {
        return isActive() && isPressed() && echest.get();
    }

    boolean previewMaps() {
        return isActive() && isPressed() && maps.get();
    }

    boolean previewBooks() {
        return isActive() && isPressed() && books.get();
    }

    boolean previewBanners() {
        return isActive() && isPressed() && banners.get();
    }

    private boolean isPressed() {
        return (keybind.get().isPressed() && displayWhen.get() == DisplayWhen.Keybind) || displayWhen.get() == DisplayWhen.Always;
    }

    @EventHandler
    private void appendTooltip(GetTooltipEvent.Append event) {
        // Item size tooltip
        if (byteSize.get()) {
            try {
                event.itemStack.writeNbt(new NbtCompound()).write(ByteCountDataOutput.INSTANCE);

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
                NbtCompound tag = event.itemStack.getTag();
                if (tag != null) {
                    NbtList effects = tag.getList("Effects", 10);
                    if (effects != null) {
                        for (int i = 0; i < effects.size(); i++) {
                            NbtCompound effectTag = effects.getCompound(i);
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
                NbtCompound tag = event.itemStack.getTag();
                if (tag != null) {
                    NbtCompound blockStateTag = tag.getCompound("BlockStateTag");
                    if (blockStateTag != null) {
                        int level = blockStateTag.getInt("honey_level");
                        event.list.add(1, new LiteralText(String.format("%sHoney level: %s%d%s.",
                            Formatting.GRAY, Formatting.YELLOW, level, Formatting.GRAY)));
                    }
                    NbtCompound blockEntityTag = tag.getCompound("BlockEntityTag");
                    if (blockEntityTag != null) {
                        NbtList beesTag = blockEntityTag.getList("Bees", 10);
                        event.list.add(1, new LiteralText(String.format("%sBees: %s%d%s.",
                            Formatting.GRAY, Formatting.YELLOW, beesTag.size(), Formatting.GRAY)));
                    }

                }
            }
        }

        // Hold to preview tooltip
        if ((Utils.hasItems(event.itemStack) && shulkers.get() && !previewShulkers())
            || (event.itemStack.getItem() == Items.ENDER_CHEST && echest.get() && !previewEChest())
            || (event.itemStack.getItem() == Items.FILLED_MAP && maps.get() && !previewMaps())
            || (event.itemStack.getItem() == Items.WRITABLE_BOOK && books.get() && !previewBooks())
            || (event.itemStack.getItem() == Items.WRITTEN_BOOK && books.get() && !previewBooks())
            || ((event.itemStack.getItem() instanceof BannerItem || event.itemStack.getItem() instanceof BannerPatternItem) && !previewBanners())) {
            event.list.add(new LiteralText(""));
            event.list.add(new LiteralText("Hold " + Formatting.YELLOW + keybind + Formatting.RESET + " to preview"));
        }

        // Remove Vanilla tooltip
        if ((event.itemStack.getItem() == Items.FILLED_MAP && previewMaps() && !showVanilla.get())
            || (event.itemStack.getItem() == Items.ENDER_CHEST && previewEChest() && !showVanilla.get())
            || (event.itemStack.getItem() == Items.WRITABLE_BOOK && previewBooks() && !showVanilla.get())
            || (event.itemStack.getItem() == Items.WRITTEN_BOOK && previewBooks() && !showVanilla.get())
            || (event.itemStack.getItem() == Items.ENDER_CHEST && previewEChest() && !showVanilla.get())
            || (Utils.hasItems(event.itemStack) && previewShulkers() && !showVanilla.get())
            || ((event.itemStack.getItem() instanceof BannerItem || event.itemStack.getItem() instanceof BannerPatternItem) && previewBanners() && !showVanilla.get())) {
            event.list.clear();
        }
    }

    @EventHandler
    private void getTooltipData(TooltipDataEvent event) {
        // Shulker Preview
        if (Utils.hasItems(event.itemStack) && previewShulkers()) {
            NbtCompound compoundTag = event.itemStack.getSubTag("BlockEntityTag");
            DefaultedList<ItemStack> itemStacks = DefaultedList.ofSize(27, ItemStack.EMPTY);
            Inventories.readNbt(compoundTag, itemStacks);
            event.tooltipData = new ContainerTooltipComponent(itemStacks, Utils.getShulkerColor(event.itemStack));
        }

        // EChest preview
        else if (event.itemStack.getItem() == Items.ENDER_CHEST && previewEChest()) {
            event.tooltipData = new ContainerTooltipComponent(EChestMemory.ITEMS, ECHEST_COLOR);
        }

        // Map preview
        else if (event.itemStack.getItem() == Items.FILLED_MAP && previewMaps()) {
            Integer mapId = FilledMapItem.getMapId(event.itemStack);
            if (mapId != null)
                event.tooltipData = new MapTooltipComponent(mapId);
        }

        // Book preview
        else if ((event.itemStack.getItem() == Items.WRITABLE_BOOK
            ||event.itemStack.getItem() == Items.WRITTEN_BOOK)
            && previewBooks()) {
            Text page = getFirstPage(event.itemStack);
            if (page != null) {
                event.tooltipData = new BookTooltipComponent(page);
            }
        }

        // Banner preview
        else if (event.itemStack.getItem() instanceof BannerItem && previewBanners()) {
            event.tooltipData = new BannerTooltipComponent(event.itemStack);
        }
        else if (event.itemStack.getItem() instanceof BannerPatternItem && previewBanners()) {
            event.tooltipData = new BannerTooltipComponent(createBannerFromPattern(
                ((BannerPatternItem)(event.itemStack.getItem())).getPattern()
            ));
        }
    }

    @EventHandler
    private void modifyTooltip(GetTooltipEvent.Modify event) {
        if ((Utils.hasItems(event.itemStack) && previewShulkers())
            || (event.itemStack.getItem() == Items.ENDER_CHEST && previewEChest())
            || (event.itemStack.getItem() == Items.FILLED_MAP && previewMaps())
            || (getFirstPage(event.itemStack) != null && books.get() && previewBooks())
            || ((event.itemStack.getItem() instanceof BannerItem || event.itemStack.getItem() instanceof BannerPatternItem) && previewBanners())) {
            event.y -= 10 * event.list.size();
            event.y -= 4;
        }
    }

    public void applyCompactShulkerTooltip(ItemStack stack, List<Text> tooltip) {
        NbtCompound tag = stack.getSubTag("BlockEntityTag");
        if (tag != null) {
            if (tag.contains("LootTable", 8)) {
                tooltip.add(new LiteralText("???????"));
            }

            if (tag.contains("Items", 9)) {
                DefaultedList<ItemStack> items = DefaultedList.ofSize(27, ItemStack.EMPTY);
                Inventories.readNbt(tag, items);

                Object2IntMap<Item> counts = new Object2IntOpenHashMap<>();

                for (ItemStack item : items) {
                    if (item.isEmpty()) continue;

                    int count = counts.getInt(item.getItem());
                    counts.put(item.getItem(), count + item.getCount());
                }

                counts.keySet().stream().sorted(Comparator.comparingInt(value -> -counts.getInt(value))).limit(5).forEach(item -> {
                    MutableText mutableText = item.getName().shallowCopy();
                    mutableText.append(new LiteralText(" x").append(String.valueOf(counts.getInt(item))).formatted(Formatting.GRAY));
                    tooltip.add(mutableText);
                });

                if (counts.size() > 5) {
                    tooltip.add((new TranslatableText("container.shulkerBox.more", counts.size() - 5)).formatted(Formatting.ITALIC));
                }
            }
        }
    }

    private MutableText getStatusText(StatusEffectInstance effect) {
        MutableText text = new TranslatableText(effect.getTranslationKey());
        if (effect.getAmplifier() != 0) {
            text.append(String.format(" %d (%s)", effect.getAmplifier() + 1, StatusEffectUtil.durationToString(effect, 1)));
        } else {
            text.append(String.format(" (%s)", StatusEffectUtil.durationToString(effect, 1)));
        }
        if (effect.getEffectType().isBeneficial()) {
            return text.formatted(Formatting.BLUE);
        } else {
            return text.formatted(Formatting.RED);
        }
    }

    private Text getFirstPage(ItemStack stack) {
        NbtCompound tag = stack.getTag();
        if (tag == null) return null;
        NbtList ltag = tag.getList("pages", 8);
        if (ltag.size() < 1) return null;
        if (stack.getItem() == Items.WRITABLE_BOOK) return new LiteralText(ltag.getString(0));
        else return Text.Serializer.fromLenientJson(ltag.getString(0));
    }

    public boolean middleClickOpen() {
        return isActive() && middleClickOpen.get();
    }

    private ItemStack createBannerFromPattern(BannerPattern pattern) {
        ItemStack itemStack = new ItemStack(Items.GRAY_BANNER);
        NbtCompound nbt = itemStack.getOrCreateSubTag("BlockEntityTag");
        NbtList listNbt = (new BannerPattern.Patterns()).add(BannerPattern.BASE, DyeColor.GRAY).add(pattern, DyeColor.WHITE).toNbt();
        nbt.put("Patterns", listNbt);
        return itemStack;
    }

    public enum DisplayWhen {
        Keybind,
        Always
    }
}
