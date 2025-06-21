/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectIntImmutablePair;
import it.unimi.dsi.fastutil.objects.ObjectIntPair;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4fStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static meteordevelopment.meteorclient.MeteorClient.mc;

// TODO: Rewrite this to use the hud renderer system
public class CombatHud extends HudElement {
    private static final Color GREEN = new Color(15, 255, 15);
    private static final Color RED = new Color(255, 15, 15);
    private static final Color BLACK = new Color(0, 0, 0, 255);

    public static final HudElementInfo<CombatHud> INFO = new HudElementInfo<>(Hud.GROUP, "combat", "Displays information about your combat target.", CombatHud::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgEnchantments = settings.createGroup("Enchantments");
    private final SettingGroup sgHealth = settings.createGroup("Health");
    private final SettingGroup sgDistance = settings.createGroup("Distance");
    private final SettingGroup sgPing = settings.createGroup("Ping");
    private final SettingGroup sgScale = settings.createGroup("Scale");
    private final SettingGroup sgBackground = settings.createGroup("Background");

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The range to target players.")
        .defaultValue(100)
        .min(1)
        .sliderMax(200)
        .build()
    );

    // Health

    private final Setting<SettingColor> healthColor1 = sgHealth.add(new ColorSetting.Builder()
        .name("health-stage-1")
        .description("The color on the left of the health gradient.")
        .defaultValue(new SettingColor(255, 15, 15))
        .build()
    );

    private final Setting<SettingColor> healthColor2 = sgHealth.add(new ColorSetting.Builder()
        .name("health-stage-2")
        .description("The color in the middle of the health gradient.")
        .defaultValue(new SettingColor(255, 150, 15))
        .build()
    );

    private final Setting<SettingColor> healthColor3 = sgHealth.add(new ColorSetting.Builder()
        .name("health-stage-3")
        .description("The color on the right of the health gradient.")
        .defaultValue(new SettingColor(15, 255, 15))
        .build()
    );

    // Enchantments

    private final Setting<Set<RegistryKey<Enchantment>>> displayedEnchantments = sgEnchantments.add(new EnchantmentListSetting.Builder()
        .name("displayed-enchantments")
        .description("The enchantments that are shown on nametags.")
        .vanillaDefaults()
        .build()
    );

    private final Setting<SettingColor> enchantmentTextColor = sgEnchantments.add(new ColorSetting.Builder()
        .name("enchantment-color")
        .description("Color of enchantment text.")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    // Ping

    private final Setting<Boolean> displayPing = sgPing.add(new BoolSetting.Builder()
        .name("ping")
        .description("Shows the player's ping.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> pingColor1 = sgPing.add(new ColorSetting.Builder()
        .name("ping-stage-1")
        .description("Color of ping text when under 75.")
        .defaultValue(new SettingColor(15, 255, 15))
        .visible(displayPing::get)
        .build()
    );

    private final Setting<SettingColor> pingColor2 = sgPing.add(new ColorSetting.Builder()
        .name("ping-stage-2")
        .description("Color of ping text when between 75 and 200.")
        .defaultValue(new SettingColor(255, 150, 15))
        .visible(displayPing::get)
        .build()
    );

    private final Setting<SettingColor> pingColor3 = sgPing.add(new ColorSetting.Builder()
        .name("ping-stage-3")
        .description("Color of ping text when over 200.")
        .defaultValue(new SettingColor(255, 15, 15))
        .visible(displayPing::get)
        .build()
    );

    // Distance

    private final Setting<Boolean> displayDistance = sgDistance.add(new BoolSetting.Builder()
        .name("distance")
        .description("Shows the distance between you and the player.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> distColor1 = sgDistance.add(new ColorSetting.Builder()
        .name("distance-stage-1")
        .description("The color when a player is within 10 blocks of you.")
        .defaultValue(new SettingColor(255, 15, 15))
        .visible(displayDistance::get)
        .build()
    );

    private final Setting<SettingColor> distColor2 = sgDistance.add(new ColorSetting.Builder()
        .name("distance-stage-2")
        .description("The color when a player is within 50 blocks of you.")
        .defaultValue(new SettingColor(255, 150, 15))
        .visible(displayDistance::get)
        .build()
    );

    private final Setting<SettingColor> distColor3 = sgDistance.add(new ColorSetting.Builder()
        .name("distance-stage-3")
        .description("The color when a player is greater then 50 blocks away from you.")
        .defaultValue(new SettingColor(15, 255, 15))
        .visible(displayDistance::get)
        .build()
    );

    // Scale

    public final Setting<Boolean> customScale = sgScale.add(new BoolSetting.Builder()
        .name("custom-scale")
        .description("Applies a custom scale to this hud element.")
        .defaultValue(false)
        .onChanged(aBoolean -> calculateSize())
        .build()
    );

    public final Setting<Double> scale = sgScale.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Custom scale.")
        .visible(customScale::get)
        .defaultValue(2)
        .onChanged(aDouble -> calculateSize())
        .min(0.5)
        .sliderRange(0.5, 3)
        .build()
    );

    // Background

    public final Setting<Boolean> background = sgBackground.add(new BoolSetting.Builder()
        .name("background")
        .description("Displays background.")
        .defaultValue(false)
        .build()
    );

    public final Setting<SettingColor> backgroundColor = sgBackground.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Color used for the background.")
        .visible(background::get)
        .defaultValue(new SettingColor(25, 25, 25, 50))
        .build()
    );

    private PlayerEntity playerEntity;

    public CombatHud() {
        super(INFO);

        calculateSize();
    }

    private void calculateSize() {
        setSize(175 * getScale(), 95 * getScale());
    }

    @Override
    public void render(HudRenderer renderer) {
        renderer.post(() -> {
            double x = this.x;
            double y = this.y;

            // TODO: These should probably be settings
            Color primaryColor = TextHud.getSectionColor(0);
            Color secondaryColor = TextHud.getSectionColor(1);

            if (isInEditor()) playerEntity = mc.player;
            else playerEntity = TargetUtils.getPlayerTarget(range.get(), SortPriority.LowestDistance);

            if (playerEntity == null && !isInEditor()) return;

            if (background.get()) {
                Renderer2D.COLOR.begin();
                Renderer2D.COLOR.quad(x, y, getWidth(), getHeight(), backgroundColor.get());
            }

            if (playerEntity == null) {
                if (isInEditor()) {
                    renderer.line(x, y, x + getWidth(), y + getHeight(), Color.GRAY);
                    renderer.line(x + getWidth(), y, x, y + getHeight(), Color.GRAY);
                    Renderer2D.COLOR.render(); // I know, ill fix it soon
                }
                return;
            }
            Renderer2D.COLOR.render();

            // Player Model
            renderer.entity(
                playerEntity,
                (int) (x + 5 * getScale()),
                (int) (y + 10 * getScale()),
                (int) (50 * getScale()),
                (int) (60 * getScale()),
                -MathHelper.wrapDegrees(playerEntity.lastYaw + (playerEntity.getYaw() - playerEntity.lastYaw) * mc.getRenderTickCounter().getTickProgress(true)),
                -playerEntity.getPitch()
            );

            // Moving pos to past player model
            x += 50 * getScale();
            y += 5 * getScale();

            // Setting up texts
            String breakText = " | ";

            // Name
            String nameText = playerEntity.getName().getString();
            Color nameColor = PlayerUtils.getPlayerColor(playerEntity, primaryColor);

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

            Color friendColor = primaryColor;

            if (Friends.get().isFriend(playerEntity)) {
                friendText = "Friend";
                friendColor = Config.get().friendColor.get();
            } else {
                boolean naked = true;

                for (int position = 3; position >= 0; position--) {
                    ItemStack itemStack = getItem(position);

                    if (!itemStack.isEmpty()) naked = false;
                }

                if (naked) {
                    friendText = "Naked";
                    friendColor = GREEN;
                } else {
                    boolean threat = false;

                    for (int position = 5; position >= 0; position--) {
                        ItemStack itemStack = getItem(position);

                        if (itemStack.isIn(ItemTags.SWORDS)
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

            TextRenderer.get().begin(0.45 * getScale(), false, true);

            double breakWidth = TextRenderer.get().getWidth(breakText);
            double pingWidth = TextRenderer.get().getWidth(pingText);
            double friendWidth = TextRenderer.get().getWidth(friendText);

            TextRenderer.get().render(nameText, x, y, nameColor != null ? nameColor : primaryColor);

            y += TextRenderer.get().getHeight();

            TextRenderer.get().render(friendText, x, y, friendColor);

            if (displayPing.get()) {
                TextRenderer.get().render(breakText, x + friendWidth, y, secondaryColor);
                TextRenderer.get().render(pingText, x + friendWidth + breakWidth, y, pingColor);

                if (displayDistance.get()) {
                    TextRenderer.get().render(breakText, x + friendWidth + breakWidth + pingWidth, y, secondaryColor);
                    TextRenderer.get().render(distText, x + friendWidth + breakWidth + pingWidth + breakWidth, y, distColor);
                }
            } else if (displayDistance.get()) {
                TextRenderer.get().render(breakText, x + friendWidth, y, secondaryColor);
                TextRenderer.get().render(distText, x + friendWidth + breakWidth, y, distColor);
            }

            TextRenderer.get().end();

            // Moving pos down for armor
            y += 10 * getScale();

            double armorX;
            double armorY;
            int slot = 5;

            // Drawing armor
            Matrix4fStack matrices = RenderSystem.getModelViewStack();

            matrices.pushMatrix();
            matrices.scale((float) getScale(), (float) getScale(), 1);

            TextRenderer.get().begin(0.35, false, true);

            for (int position = 0; position < 6; position++) {
                armorX = x + (position * 20 * getScale());
                armorY = y;

                ItemStack itemStack = getItem(slot);

                renderer.item(itemStack, (int) (armorX), (int) (armorY), (float) getScale(), true);

                armorY = (y / getScale()) + 18;

                ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(itemStack);
                List<ObjectIntPair<RegistryEntry<Enchantment>>> enchantmentsToShow = new ArrayList<>();

                for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchantments.getEnchantmentEntries()) {
                    if (entry.getKey().matches(displayedEnchantments.get()::contains)) {
                        enchantmentsToShow.add(new ObjectIntImmutablePair<>(entry.getKey(), entry.getIntValue()));
                    }
                }

                for (ObjectIntPair<RegistryEntry<Enchantment>> entry : enchantmentsToShow) {
                    String enchantName = Utils.getEnchantSimpleName(entry.left(), 3) + " " + entry.rightInt();

                    double enchX = (((x / getScale()) + position * 20) + 8) - (TextRenderer.get().getWidth(enchantName) / 2);
                    TextRenderer.get().render(enchantName, enchX, armorY, entry.left().isIn(EnchantmentTags.CURSE) ? RED : enchantmentTextColor.get());
                    armorY += TextRenderer.get().getHeight();
                }
                slot--;
            }

            TextRenderer.get().end();

            y = (int) (this.y + 75 * getScale());
            x = this.x;

            // Health bar

            x /= getScale();
            y /= getScale();

            x += 5;
            y += 5;

            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.boxLines(x, y, 165, 11, BLACK);
            Renderer2D.COLOR.render();

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
            Renderer2D.COLOR.render();

            matrices.popMatrix();
        });
    }

    private ItemStack getItem(int i) {
        if (isInEditor()) {
            return switch (i) {
                case 0 -> Items.NETHERITE_BOOTS.getDefaultStack();
                case 1 -> Items.NETHERITE_LEGGINGS.getDefaultStack();
                case 2 -> Items.NETHERITE_CHESTPLATE.getDefaultStack();
                case 3 -> Items.NETHERITE_HELMET.getDefaultStack();
                case 4 -> Items.TOTEM_OF_UNDYING.getDefaultStack();
                case 5 -> Items.END_CRYSTAL.getDefaultStack();
                default -> ItemStack.EMPTY;
            };
        }

        if (playerEntity == null) return ItemStack.EMPTY;

        return switch (i) {
            case 5 -> playerEntity.getMainHandStack();
            case 4 -> playerEntity.getOffHandStack();
            case 3 -> playerEntity.getEquippedStack(EquipmentSlot.HEAD);
            case 2 -> playerEntity.getEquippedStack(EquipmentSlot.CHEST);
            case 1 -> playerEntity.getEquippedStack(EquipmentSlot.LEGS);
            case 0 -> playerEntity.getEquippedStack(EquipmentSlot.FEET);
            default -> ItemStack.EMPTY;
        };
    }

    private double getScale() {
        return customScale.get() ? scale.get() : Hud.get().getTextScale();
    }
}
