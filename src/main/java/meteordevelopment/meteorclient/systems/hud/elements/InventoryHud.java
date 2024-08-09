/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements;

import static meteordevelopment.meteorclient.MeteorClient.mc;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Nametags;
import meteordevelopment.meteorclient.systems.modules.render.ESP;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import java.util.*;

public class InventoryHud extends HudElement {
    public static final HudElementInfo<InventoryHud> INFO = new HudElementInfo<>(Hud.GROUP, "inventory", "Displays your inventory.", InventoryHud::new);

    private static final Identifier TEXTURE = MeteorClient.identifier("textures/container.png");
    private static final Identifier TEXTURE_TRANSPARENT = MeteorClient.identifier("textures/container-transparent.png");

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> containers = sgGeneral.add(new BoolSetting.Builder()
        .name("containers")
        .description("Shows the contents of a container when holding them.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> isAutoconf = sgGeneral.add(new BoolSetting.Builder()
        .name("autoconfigure-module")
        .description("Automatically enables relevant features of a selected module")
        .defaultValue(true)
        .build()
    );

    private final Setting<SupportedSources> selectedSource = sgGeneral.add(new EnumSetting.Builder<SupportedSources>()
        .name("selected-source")
        .description("From which source to display items.")
        .defaultValue(SupportedSources.Inventory)
        .onChanged(source -> {
            if (isAutoconf.get()) {
                source.configure();
            }
        })
        .build()
    );

    private int containerItemsWidth = 9;
    private int containerItemsHeight = 3;
    private boolean assumeDefaultSize = true;

    public final Setting<Integer> sourceContainerWidth = sgGeneral.add(new IntSetting.Builder()
        .name("source-container-width")
        .description("Maximum width of the input container")
        .defaultValue(9)
        .onChanged(integer -> {
            resizeContainerItems(integer, containerItemsHeight);
        })
        .min(1)
        .sliderRange(1, 20)
        .build()
    );

    public final Setting<Integer> sourceContainerHeight = sgGeneral.add(new IntSetting.Builder()
        .name("source-container-height")
        .description("Maximum height of the input container")
        .defaultValue(3)
        .onChanged(integer -> {
            resizeContainerItems(containerItemsWidth, integer);
        })
        .min(1)
        .sliderRange(1, 20)
        .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The scale.")
        .defaultValue(2)
        .min(1)
        .sliderRange(1, 5)
        .build()
    );

    private final Setting<Background> background = sgGeneral.add(new EnumSetting.Builder<Background>()
        .name("background")
        .description("Background of inventory viewer.")
        .defaultValue(Background.Texture)
        .build()
    );

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Color of the background.")
        .defaultValue(new SettingColor(255, 255, 255))
        .visible(() -> background.get() != Background.None)
        .build()
    );

    private ArrayList<ItemStack> containerItems = new ArrayList<>();
    void resizeContainerItems(int newWidth, int newHeight) {
        int size = newWidth * newHeight;
        containerItems = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            containerItems.add(null);
        }
        containerItemsWidth = newWidth;
        containerItemsHeight = newHeight;
    }

    private InventoryHud() {
        super(INFO);

        resizeContainerItems(9, 3);
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = this.x, y = this.y;

        ItemStack container = getContainer();
        boolean hasContainer = containers.get() && container != null;
        if (hasContainer) {
            assumeDefaultSize = true;
            ItemStack[] containerItemsArray = new ItemStack[3 * 9];
            Utils.getItemsInContainerItem(container, containerItemsArray);
            containerItems = new ArrayList<>(Arrays.asList(containerItemsArray));
        }
        else {
            assumeDefaultSize = false;
            resizeContainerItems(containerItemsWidth, containerItemsHeight);
            ArrayList<ItemStack> items = selectedSource.get().getItems();
            for (int i = 0; i < containerItems.size(); ++i) {
                containerItems.set(i, i < items.size() ? items.get(i) : null);
            }
        }

        int w = (assumeDefaultSize ? 9 : containerItemsWidth);
        int h = (assumeDefaultSize ? 3 : containerItemsHeight);
        calculateSize(w, h);

        Color drawColor = hasContainer ? Utils.getShulkerColor(container) : color.get();
        if (background.get() != Background.None) {
            drawBackground(renderer, (int) x, (int) y, drawColor);
        }

        if (mc.player == null) return;

        renderer.post(() -> {
            for (int row = 0; row < h; row++) {
                for (int i = 0; i < w; i++) {
                    int index = row * w + i;
                    ItemStack stack = containerItems.get(index);
                    if (stack == null) continue;

                    int itemX = background.get() == Background.Texture ? (int) (x + (8 + i * 18) * scale.get()) : (int) (x + (1 + i * 18) * scale.get());
                    int itemY = background.get() == Background.Texture ? (int) (y + (7 + row * 18) * scale.get()) : (int) (y + (1 + row * 18) * scale.get());

                    renderer.item(stack, itemX, itemY, scale.get().floatValue(), true);
                }
            }
        });
    }

    private void calculateSize(int w, int h) {
        if (background.get() == Background.Texture) {
            setSize((8 + (w - 1) * 18 + 24) * scale.get(), (7 + (h - 1) * 18 + 24) * scale.get());
        } else {
            setSize((1 + (w - 1) * 18 + 17) * scale.get(), (1 + (h - 1) * 18 + 17) * scale.get());
        }
    }

    private void drawBackground(HudRenderer renderer, int x, int y, Color color) {
        int w = getWidth();
        int h = getHeight();

        // TODO: Generate Texture and Outline textures for arbitrary inventory sizes.
        if ((assumeDefaultSize ? 9 : containerItemsWidth) != 9 || (assumeDefaultSize ? 3 : containerItemsHeight) != 3 || background.get() == Background.Flat) {
            renderer.quad(x, y, w, h, color);
        } else {
            renderer.texture(background.get() == Background.Texture ? TEXTURE : TEXTURE_TRANSPARENT, x, y, w, h, color);
        }
        /* switch (background.get()) {
            case Texture, Outline -> renderer.texture(background.get() == Background.Texture ? TEXTURE : TEXTURE_TRANSPARENT, x, y, w, h, color);
            case Flat -> renderer.quad(x, y, w, h, color);
        } */
    }

    private ItemStack getContainer() {
        if (isInEditor() || mc.player == null) return null;

        ItemStack stack = mc.player.getOffHandStack();
        if (Utils.hasItems(stack) || stack.getItem() == Items.ENDER_CHEST) return stack;

        stack = mc.player.getMainHandStack();
        if (Utils.hasItems(stack) || stack.getItem() == Items.ENDER_CHEST) return stack;

        return null;
    }

    private enum SupportedSources {
        None {
        },
        Inventory {
            @Override
            public ArrayList<ItemStack> getItems() {
                if (mc.player == null) return new ArrayList<>();
                ArrayList<ItemStack> result = new ArrayList<>(3 * 9);
                for (int row = 0; row < 3; row++) {
                    for (int i = 0; i < 9; i++) {
                        result.add(mc.player.getInventory().getStack((row + 1) * 9 + i));
                    }
                }
                return result;
            }
        },
        Nametags {
            @Override
            public void configure() {
                if (!Modules.get().get(Nametags.class).isActive()) Modules.get().get(Nametags.class).toggle();
                ((Set<EntityType<?>>) Modules.get().get(Nametags.class).settings.getGroup("General").get("entities").get()).add(EntityType.ITEM);
            }
            @Override
            public ArrayList<ItemStack> getItems() { return Modules.get().get(Nametags.class).getItems(); }
        },
        ESP {
            @Override
            public void configure() {
                if (!Modules.get().get(ESP.class).isActive()) Modules.get().get(ESP.class).toggle();
                ((Set<EntityType<?>>) Modules.get().get(ESP.class).settings.getGroup("General").get("entities").get()).add(EntityType.ITEM);
            }
            @Override
            public ArrayList<ItemStack> getItems() { return Modules.get().get(ESP.class).getItems(); }
        };
        public void configure() {}
        public ArrayList<ItemStack> getItems() {
            return new ArrayList<>();
        }
    };

    public enum Background {
        None(162, 54),
        Texture(176, 67),
        Outline(162, 54),
        Flat(162, 54);

        private final int width, height;

        Background(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
}
