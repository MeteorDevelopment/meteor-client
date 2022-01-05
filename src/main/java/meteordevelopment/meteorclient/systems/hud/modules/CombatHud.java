/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.modules;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.misc.FakeClientPlayer;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CombatHud extends HudElement {
    private static final Color GREEN = new Color(15, 255, 15);
    private static final Color RED = new Color(255, 15, 15);
    private static final Color BLACK = new Color(0, 0, 0, 255);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The scale.")
        .defaultValue(2)
        .min(1)
        .sliderRange(1, 5)
        .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The range to target players.")
        .defaultValue(100)
        .min(1)
        .sliderMax(200)
        .build()
    );

    private final Setting<Boolean> displayPing = sgGeneral.add(new BoolSetting.Builder()
        .name("ping")
        .description("Shows the player's ping.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> displayDistance = sgGeneral.add(new BoolSetting.Builder()
        .name("distance")
        .description("Shows the distance between you and the player.")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<Enchantment>> displayedEnchantments = sgGeneral.add(new EnchantmentListSetting.Builder()
        .name("displayed-enchantments")
        .description("The enchantments that are shown on nametags.")
        .defaultValue(getDefaultEnchantments())
        .build()
    );

    private final Setting<SettingColor> backgroundColor = sgGeneral.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Color of background.")
        .defaultValue(new SettingColor(0, 0, 0, 64))
        .build()
    );

    private final Setting<SettingColor> enchantmentTextColor = sgGeneral.add(new ColorSetting.Builder()
        .name("enchantment-color")
        .description("Color of enchantment text.")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    private final Setting<SettingColor> pingColor1 = sgGeneral.add(new ColorSetting.Builder()
        .name("ping-stage-1")
        .description("Color of ping text when under 75.")
        .defaultValue(new SettingColor(15, 255, 15))
        .visible(displayPing::get)
        .build()
    );

    private final Setting<SettingColor> pingColor2 = sgGeneral.add(new ColorSetting.Builder()
        .name("ping-stage-2")
        .description("Color of ping text when between 75 and 200.")
        .defaultValue(new SettingColor(255, 150, 15))
        .visible(displayPing::get)
        .build()
    );

    private final Setting<SettingColor> pingColor3 = sgGeneral.add(new ColorSetting.Builder()
        .name("ping-stage-3")
        .description("Color of ping text when over 200.")
        .defaultValue(new SettingColor(255, 15, 15))
        .visible(displayPing::get)
        .build()
    );

    private final Setting<SettingColor> distColor1 = sgGeneral.add(new ColorSetting.Builder()
        .name("distance-stage-1")
        .description("The color when a player is within 10 blocks of you.")
        .defaultValue(new SettingColor(255, 15, 15))
        .visible(displayDistance::get)
        .build()
    );

    private final Setting<SettingColor> distColor2 = sgGeneral.add(new ColorSetting.Builder()
        .name("distance-stage-2")
        .description("The color when a player is within 50 blocks of you.")
        .defaultValue(new SettingColor(255, 150, 15))
        .visible(displayDistance::get)
        .build()
    );

    private final Setting<SettingColor> distColor3 = sgGeneral.add(new ColorSetting.Builder()
        .name("distance-stage-3")
        .description("The color when a player is greater then 50 blocks away from you.")
        .defaultValue(new SettingColor(15, 255, 15))
        .visible(displayDistance::get)
        .build()
    );

    private final Setting<SettingColor> healthColor1 = sgGeneral.add(new ColorSetting.Builder()
        .name("health-stage-1")
        .description("The color on the left of the health gradient.")
        .defaultValue(new SettingColor(255, 15, 15))
        .build()
    );

    private final Setting<SettingColor> healthColor2 = sgGeneral.add(new ColorSetting.Builder()
        .name("health-stage-2")
        .description("The color in the middle of the health gradient.")
        .defaultValue(new SettingColor(255, 150, 15))
        .build()
    );

    private final Setting<SettingColor> healthColor3 = sgGeneral.add(new ColorSetting.Builder()
        .name("health-stage-3")
        .description("The color on the right of the health gradient.")
        .defaultValue(new SettingColor(15, 255, 15))
        .build()
    );

    private PlayerEntity playerEntity;

    public CombatHud(HUD hud) {
        super(hud, "combat-info", "Displays information about your combat target.", false);
    }

    @Override
    public void update(HudRenderer renderer) {
        box.setSize(175 * scale.get(), 95 * scale.get());
    }

    @Override
    public void render(HudRenderer renderer) {
        renderer.addPostTask(() -> {
            double x = box.getX();
            double y = box.getY();

            if (isInEditor()) playerEntity = FakeClientPlayer.getPlayer();
            else playerEntity = TargetUtils.getPlayerTarget(range.get(), SortPriority.LowestDistance);

            if (playerEntity == null) return;

            // Background
            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.quad(x, y, box.width, box.height, backgroundColor.get());
            Renderer2D.COLOR.render(null);

            // Player Model
            InventoryScreen.drawEntity(
                (int) (x + (25 * scale.get())),
                (int) (y + (66 * scale.get())),
                (int) (30 * scale.get()),
                -MathHelper.wrapDegrees(playerEntity.prevYaw + (playerEntity.getYaw() - playerEntity.prevYaw) * mc.getTickDelta()),
                -playerEntity.getPitch(), playerEntity
            );

            // Moving pos to past player model
            x += 50 * scale.get();
            y += 5 * scale.get();

            // Setting up texts
            String breakText = " | ";

            // Name
            String nameText = playerEntity.getEntityName();
            Color nameColor = PlayerUtils.getPlayerColor(playerEntity, hud.primaryColor.get());

            // Ping
            int ping = EntityUtils.getPing(playerEntity);
            String pingText = ping + "ms";

            Color pingColor;
            if (ping <= 75) pingColor = pingColor1.get();
            else if (ping <= 200) pingColor = pingColor2.get();
            else pingColor = pingColor3.get();

            // Distance
            double dist = 0;
            if (!isInEditor()) dist = Math.round(mc.player.distanceTo(playerEntity) * 100.0) / 100.0;
            String distText = dist + "m";

            Color distColor;
            if (dist <= 10) distColor = distColor1.get();
            else if (dist <= 50) distColor = distColor2.get();
            else distColor = distColor3.get();

            // Status Text
            String friendText = "Unknown";

            Color friendColor = hud.primaryColor.get();

            if (Friends.get().isFriend(playerEntity)) {
                friendText = "Friend";
                friendColor = Friends.get().color;
            }
            else {
                boolean naked = true;

                for (int position = 3; position >= 0; position--) {
                    ItemStack itemStack = getItem(position);

                    if (!itemStack.isEmpty()) naked = false;
                }

                if (naked) {
                    friendText = "Naked";
                    friendColor = GREEN;
                }
                else {
                    boolean threat = false;

                    for (int position = 5; position >= 0; position--) {
                        ItemStack itemStack = getItem(position);

                        if (itemStack.getItem() instanceof SwordItem
                            || itemStack.getItem() == Items.END_CRYSTAL
                            || itemStack.getItem() == Items.RESPAWN_ANCHOR
                            || itemStack.getItem() instanceof BedItem) threat = true;
                    }

                    if (threat) {
                        friendText = "Threat";
                        friendColor = RED;
                    }
                }
            }

            TextRenderer.get().begin(0.45 * scale.get(), false, true);

            double breakWidth = TextRenderer.get().getWidth(breakText);
            double pingWidth = TextRenderer.get().getWidth(pingText);
            double friendWidth = TextRenderer.get().getWidth(friendText);

            TextRenderer.get().render(nameText, x, y, nameColor != null ? nameColor : hud.primaryColor.get());

            y += TextRenderer.get().getHeight();

            TextRenderer.get().render(friendText, x, y, friendColor);

            if (displayPing.get()) {
                TextRenderer.get().render(breakText, x + friendWidth, y, hud.secondaryColor.get());
                TextRenderer.get().render(pingText, x + friendWidth + breakWidth, y, pingColor);

                if (displayDistance.get()) {
                    TextRenderer.get().render(breakText, x + friendWidth + breakWidth + pingWidth, y, hud.secondaryColor.get());
                    TextRenderer.get().render(distText, x + friendWidth + breakWidth + pingWidth + breakWidth, y, distColor);
                }
            } else if (displayDistance.get()) {
                TextRenderer.get().render(breakText, x + friendWidth, y, hud.secondaryColor.get());
                TextRenderer.get().render(distText, x + friendWidth + breakWidth, y, distColor);
            }

            TextRenderer.get().end();

            // Moving pos down for armor
            y += 10 * scale.get();

            double armorX;
            double armorY;
            int slot = 5;

            // Drawing armor
            MatrixStack matrices = RenderSystem.getModelViewStack();

            matrices.push();
            matrices.scale(scale.get().floatValue(), scale.get().floatValue(), 1);

            x /= scale.get();
            y /= scale.get();

            TextRenderer.get().begin(0.35, false, true);

            for (int position = 0; position < 6; position++) {
                armorX = x + position * 20;
                armorY = y;

                ItemStack itemStack = getItem(slot);

                RenderUtils.drawItem(itemStack, (int) armorX, (int) armorY, true);

                armorY += 18;

                Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(itemStack);
                Map<Enchantment, Integer> enchantmentsToShow = new HashMap<>();

                for (Enchantment enchantment : displayedEnchantments.get()) {
                    if (enchantments.containsKey(enchantment)) {
                        enchantmentsToShow.put(enchantment, enchantments.get(enchantment));
                    }
                }

                for (Enchantment enchantment : enchantmentsToShow.keySet()) {
                    String enchantName = Utils.getEnchantSimpleName(enchantment, 3) + " " + enchantmentsToShow.get(enchantment);

                    double enchX = (armorX + 8) - (TextRenderer.get().getWidth(enchantName) / 2);

                    TextRenderer.get().render(enchantName, enchX, armorY, enchantment.isCursed() ? RED : enchantmentTextColor.get());
                    armorY += TextRenderer.get().getHeight();
                }
                slot--;
            }

            TextRenderer.get().end();

            y = (int) (box.getY() + 75 * scale.get());
            x = box.getX();

            // Health bar

            x /= scale.get();
            y /= scale.get();

            x += 5;
            y += 5;

            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.boxLines(x, y, 165, 11, BLACK);
            Renderer2D.COLOR.render(null);

            x += 2;
            y += 2;

            float maxHealth = playerEntity.getMaxHealth();
            int maxAbsorb = 16;
            int maxTotal = (int) (maxHealth + maxAbsorb);

            int totalHealthWidth = (int) (161 * maxHealth / maxTotal);
            int totalAbsorbWidth = 161 * maxAbsorb / maxTotal;

            float health = playerEntity.getHealth();
            float absorb = playerEntity.getAbsorptionAmount();

            double healthPercent = health / maxHealth;
            double absorbPercent = absorb / maxAbsorb;

            int healthWidth = (int) (totalHealthWidth * healthPercent);
            int absorbWidth = (int) (totalAbsorbWidth * absorbPercent);

            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.quad(x, y, healthWidth, 7, healthColor1.get(), healthColor2.get(), healthColor2.get(), healthColor1.get());
            Renderer2D.COLOR.quad(x + healthWidth, y, absorbWidth, 7, healthColor2.get(), healthColor3.get(), healthColor3.get(), healthColor2.get());
            Renderer2D.COLOR.render(null);

            matrices.pop();
        });
    }

    private ItemStack getItem(int i) {
        if (isInEditor()) {
            return switch (i) {
                case 0 -> Items.END_CRYSTAL.getDefaultStack();
                case 1 -> Items.NETHERITE_BOOTS.getDefaultStack();
                case 2 -> Items.NETHERITE_LEGGINGS.getDefaultStack();
                case 3 -> Items.NETHERITE_CHESTPLATE.getDefaultStack();
                case 4 -> Items.NETHERITE_HELMET.getDefaultStack();
                case 5 -> Items.TOTEM_OF_UNDYING.getDefaultStack();
                default -> ItemStack.EMPTY;
            };
        }

        if (playerEntity == null) return ItemStack.EMPTY;

        return switch (i) {
            case 4 -> playerEntity.getOffHandStack();
            case 5 -> playerEntity.getMainHandStack();
            default -> playerEntity.getInventory().getArmorStack(i);
        };
    }

    public static List<Enchantment> getDefaultEnchantments() {
        List<Enchantment> enchantments = new ArrayList<>();

        for (Enchantment enchantment : Registry.ENCHANTMENT) {
            enchantments.add(enchantment);
        }

        return enchantments;
    }
}
