/*
 *
 *  * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 *  * Copyright (c) 2021 Meteor Development.
 *
 */

package minegame159.meteorclient.modules.render.hud.modules;

import com.mojang.blaze3d.systems.RenderSystem;
import minegame159.meteorclient.friends.Friend;
import minegame159.meteorclient.friends.Friends;
import minegame159.meteorclient.modules.render.hud.HUD;
import minegame159.meteorclient.modules.render.hud.HudEditorScreen;
import minegame159.meteorclient.modules.render.hud.HudRenderer;
import minegame159.meteorclient.rendering.DrawMode;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.rendering.text.TextRenderer;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.entity.EntityUtils;
import minegame159.meteorclient.utils.entity.SortPriority;
import minegame159.meteorclient.utils.render.RenderUtils;
import minegame159.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.VertexFormats;
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

public class CombatHud extends HudModule {
    private final Color GREEN = new Color(15, 255, 15);
    private final Color RED = new Color(255, 15, 15);
    private final Color BLACK = new Color(0, 0, 0, 255);

    private PlayerEntity playerEntity;

    public CombatHud(HUD hud) {
        super(hud, "combat-info", "Displays information about your combat target.");
    }

    @Override
    public void update(HudRenderer renderer) {
        box.setSize(175 * hud.combatInfoScale.get(), 95 * hud.combatInfoScale.get());
    }

    @Override
    public void render(HudRenderer renderer) {
        renderer.addPostTask(() -> {
            int x = box.getX();
            int y = box.getY();

            if (mc.currentScreen instanceof HudEditorScreen) {
                Renderer.NORMAL.begin(null, DrawMode.Triangles, VertexFormats.POSITION_COLOR);
                Renderer.NORMAL.quad(x, y, box.width, box.height, hud.combatInfoBackgroundColor.get());
                Renderer.NORMAL.end();
            }

            playerEntity = EntityUtils.getPlayerTarget(hud.combatInfoRange.get(), SortPriority.LowestDistance, hud.combatInfoIgnoreFriends.get());
            if (playerEntity == null) return;

            //Background
            if (!(mc.currentScreen instanceof HudEditorScreen)) {
                Renderer.NORMAL.begin(null, DrawMode.Triangles, VertexFormats.POSITION_COLOR);
                Renderer.NORMAL.quad(x, y, box.width, box.height, hud.combatInfoBackgroundColor.get());
                Renderer.NORMAL.end();
            }

            //Player Model
            InventoryScreen.drawEntity(
                    x + (int) (25 * hud.combatInfoScale.get()),
                    y + (int) (66 * hud.combatInfoScale.get()),
                    (int) (30 * hud.combatInfoScale.get()),
                    -MathHelper.wrapDegrees(playerEntity.prevYaw + (playerEntity.yaw - playerEntity.prevYaw) * mc.getTickDelta()),
                    -playerEntity.pitch, playerEntity
            );

            //Moving pos to past player model
            x += 50 * hud.combatInfoScale.get();
            y += 5 * hud.combatInfoScale.get();

            //Setting up texts
            String breakText = " | ";

            //Name
            String nameText = playerEntity.getGameProfile().getName();
            Color nameColor = Friends.get().getFriendColor(playerEntity);

            //Ping
            int ping = EntityUtils.getPing(playerEntity);
            String pingText = ping + "ms";

            Color pingColor;
            if (ping <= 75) pingColor = hud.combatInfoPingColor1.get();
            else if (ping <= 200) pingColor = hud.combatInfoPingColor2.get();
            else pingColor = hud.combatInfoPingColor3.get();

            //Distance
            double dist = Math.round(mc.player.distanceTo(playerEntity) * 100.0) / 100.0;
            String distText = dist + "m";

            Color distColor;
            if (dist <= 10) distColor = hud.combatInfoDistColor1.get();
            else if (dist <= 50) distColor = hud.combatInfoDistColor2.get();
            else distColor = hud.combatInfoDistColor3.get();

            //Status Text
            String friendText = "Unknown";

            Color friendColor = hud.primaryColor.get();
            if (Friends.get().get(playerEntity) != null) {
                Friend player = Friends.get().get(playerEntity);
                friendText = player.type.name();
                friendColor = Friends.get().getFriendColor(playerEntity);
            } else {
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

                        if (itemStack.getItem() == Items.END_CRYSTAL
                                || itemStack.getItem() instanceof SwordItem
                                || itemStack.getItem() == Items.RESPAWN_ANCHOR
                                || itemStack.getItem() instanceof BedItem) threat = true;
                    }

                    if (threat) {
                        friendText = "Threat";
                        friendColor = RED;
                    }
                }
            }

            TextRenderer.get().begin(0.45 * hud.combatInfoScale.get(), false, true);

            double breakWidth = TextRenderer.get().getWidth(breakText);
            double pingWidth = TextRenderer.get().getWidth(pingText);
            double friendWidth = TextRenderer.get().getWidth(friendText);

            TextRenderer.get().render(nameText, x, y, nameColor != null ? nameColor : hud.primaryColor.get());

            y += TextRenderer.get().getHeight();

            TextRenderer.get().render(friendText, x, y, friendColor);

            if (hud.combatInfoDisplayPing.get()) {
                TextRenderer.get().render(breakText, x + friendWidth, y, hud.secondaryColor.get());
                TextRenderer.get().render(pingText, x + friendWidth + breakWidth, y, pingColor);

                if (hud.combatInfoDisplayDist.get()) {
                    TextRenderer.get().render(breakText, x + friendWidth + breakWidth + pingWidth, y, hud.secondaryColor.get());
                    TextRenderer.get().render(distText, x + friendWidth + breakWidth + pingWidth + breakWidth, y, distColor);
                }
            } else if (hud.combatInfoDisplayDist.get()) {
                TextRenderer.get().render(breakText, x + friendWidth, y, hud.secondaryColor.get());
                TextRenderer.get().render(distText, x + friendWidth + breakWidth, y, distColor);
            }

            TextRenderer.get().end();

            //Moving pos down for armor
            y += 10 * hud.combatInfoScale.get();

            double armorX;
            double armorY;
            int slot = 5;

            //Drawing armor
            RenderSystem.pushMatrix();
            RenderSystem.scaled(hud.combatInfoScale.get(), hud.combatInfoScale.get(), 1);

            x /= hud.combatInfoScale.get();
            y /= hud.combatInfoScale.get();

            TextRenderer.get().begin(0.35, false, true);

            for (int position = 0; position < 6; position++) {
                armorX = x + position * 20;
                armorY = y;

                ItemStack itemStack = getItem(slot);

                RenderUtils.drawItem(itemStack, (int) armorX, (int) armorY, true);

                armorY += 18;

                Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(itemStack);
                Map<Enchantment, Integer> enchantmentsToShow = new HashMap<>();

                for (Enchantment enchantment : hud.combatInfoDisplayedEnchantments.get()) {
                    if (enchantments.containsKey(enchantment)) enchantmentsToShow.put(enchantment, enchantments.get(enchantment));
                }

                for (Enchantment enchantment : enchantmentsToShow.keySet()) {
                    String enchantName = Utils.getEnchantSimpleName(enchantment, 3) + " " + enchantmentsToShow.get(enchantment);

                    double enchX = (armorX + 8) - (TextRenderer.get().getWidth(enchantName) / 2);

                    TextRenderer.get().render(enchantName, enchX, armorY, enchantment.isCursed() ? RED :  hud.combatInfoEnchantmentTextColor.get());
                    armorY += TextRenderer.get().getHeight();
                }
                slot--;
            }

            TextRenderer.get().end();
            RenderSystem.popMatrix();

            y = (int) (box.getY() + 75 * hud.combatInfoScale.get());
            x = box.getX();

            //Health bar
            RenderSystem.pushMatrix();
            RenderSystem.scaled(hud.combatInfoScale.get(), hud.combatInfoScale.get(), 1);

            x /= hud.combatInfoScale.get();
            y /= hud.combatInfoScale.get();

            x += 5;
            y += 5;

            Renderer.LINES.begin(null, DrawMode.Lines, VertexFormats.POSITION_COLOR);
            Renderer.LINES.boxEdges(x, y, 165, 11, BLACK);
            Renderer.LINES.end();

            x += 2;
            y += 2;

            float maxHealth = playerEntity.getMaxHealth();
            int maxAbsorb = 16;
            int maxTotal = (int) (maxHealth + maxAbsorb);

            int totalHealthWidth = (int) (161 * maxHealth / maxTotal);
            int totalAbsorbWidth = 161 * maxAbsorb / maxTotal;

            float health = playerEntity.getHealth();
            float absorb = playerEntity.getAbsorptionAmount();
            float totalHealth = health + absorb;

            double healthPrecent = health / maxHealth;
            double absorbPrecent = absorb / maxAbsorb;

            int healthWidth = (int) (totalHealthWidth * healthPrecent);
            int absorbWidth = (int) (totalAbsorbWidth * absorbPrecent);

            Renderer.NORMAL.begin(null, DrawMode.Triangles, VertexFormats.POSITION_COLOR);
            Renderer.NORMAL.gradientQuad(x, y, healthWidth, 7, hud.combatInfoHealthColor1.get(), hud.combatInfoHealthColor2.get());
            Renderer.NORMAL.gradientQuad(x + healthWidth, y, absorbWidth, 7, hud.combatInfoHealthColor2.get(), hud.combatInfoHealthColor3.get());
            Renderer.NORMAL.end();

            String healthText = String.valueOf(Math.round(totalHealth * 10.0) / 10.0);

            TextRenderer.get().begin(0.45);
            TextRenderer.get().render(healthText, x, y, hud.primaryColor.get());
            TextRenderer.get().end();

            RenderSystem.popMatrix();
        });
    }

    private ItemStack getItem(int i) {
        if (mc.currentScreen instanceof HudEditorScreen) {
            switch (i) {
                case 0:  return Items.END_CRYSTAL.getDefaultStack();
                case 1:  return Items.NETHERITE_BOOTS.getDefaultStack();
                case 2:  return Items.NETHERITE_LEGGINGS.getDefaultStack();
                case 3:  return Items.NETHERITE_CHESTPLATE.getDefaultStack();
                case 4:  return Items.NETHERITE_HELMET.getDefaultStack();
                case 5:  return Items.TOTEM_OF_UNDYING.getDefaultStack();
            }
        }

        if (i == 5) return playerEntity.getMainHandStack();
        else if (i == 4) return playerEntity.getOffHandStack();
        return playerEntity.inventory.getArmorStack(i);
    }

    public static List<Enchantment> setDefualtList() {
        List<Enchantment> ench = new ArrayList<>();
        for (Enchantment enchantment : Registry.ENCHANTMENT) {
            ench.add(enchantment);
        }
        return ench;
    }
}
