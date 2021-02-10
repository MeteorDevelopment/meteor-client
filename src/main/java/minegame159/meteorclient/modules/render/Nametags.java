/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.render;

//Updated by squidoodly 03/07/2020
//Updated by squidoodly 30/07/2020

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.friends.Friends;
import minegame159.meteorclient.mixininterface.IBakedQuad;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.modules.player.FakePlayer;
import minegame159.meteorclient.modules.player.NameProtect;
import minegame159.meteorclient.rendering.DrawMode;
import minegame159.meteorclient.rendering.MeshBuilder;
import minegame159.meteorclient.rendering.text.TextRenderer;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.entity.FakePlayerEntity;
import minegame159.meteorclient.utils.entity.FakePlayerUtils;
import minegame159.meteorclient.utils.render.NametagUtils;
import minegame159.meteorclient.utils.render.color.Color;
import minegame159.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeableArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Nametags extends Module {
    public enum Position {
        Above,
        OnTop
    }

    private static final MeshBuilder MB = new MeshBuilder(2048);

    private static final Color BACKGROUND = new Color(0, 0, 0, 75);
    private static final Color WHITE = new Color(255, 255, 255);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");

    // General

    private final Setting<Boolean> displayArmor = sgGeneral.add(new BoolSetting.Builder()
            .name("display-armor")
            .description("Displays armor.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> displayArmorEnchants = sgGeneral.add(new BoolSetting.Builder()
            .name("display-armor-enchants")
            .description("Displays armor enchantments.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Position> displayOnItem = sgGeneral.add(new EnumSetting.Builder<Position>()
            .name("enchantment-position")
            .description("Where the enchantments are rendered.")
            .defaultValue(Position.OnTop)
            .build()
    );

    private final Setting<List<Enchantment>> displayedEnchantments = sgGeneral.add(new EnchListSetting.Builder()
            .name("displayed-enchantments")
            .description("The enchantments that are shown on nametags.")
            .defaultValue(setDefualtList())
            .build()
    );

    private final Setting<Boolean> displayPing = sgGeneral.add(new BoolSetting.Builder()
            .name("ping")
            .description("Shows the player's ping.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale")
            .description("The scale.")
            .defaultValue(1)
            .min(0.1)
            .build()
    );

    private final Setting<Double> enchantTextScale = sgGeneral.add(new DoubleSetting.Builder()
            .name("enchant-text-scale")
            .description("The scale of the enchantment text.")
            .defaultValue(0.6)
            .min(0.1)
            .max(1)
            .sliderMin(0.1)
            .sliderMax(1)
            .build()
    );

    private final Setting<Boolean> yourself = sgGeneral.add(new BoolSetting.Builder()
            .name("self-nametag")
            .description("Displays a nametag on your player if you're in Freecam.")
            .defaultValue(true)
            .build()
    );

    // Colors

    private final Setting<SettingColor> normalName = sgColors.add(new ColorSetting.Builder()
            .name("normal-color")
            .description("The color of people not in your Friends List.")
            .defaultValue(new SettingColor(255, 255, 255))
            .build()
    );

    private final Setting<SettingColor> pingColor = sgColors.add(new ColorSetting.Builder()
            .name("ping-color")
            .description("The color of the ping text.")
            .defaultValue(new SettingColor(150, 150, 150))
            .build()
    );

    private final Setting<SettingColor> healthStage1 = sgColors.add(new ColorSetting.Builder()
            .name("health-stage-1")
            .description("The color if a player is full health.")
            .defaultValue(new SettingColor(25, 252, 25))
            .build()
    );

    private final Setting<SettingColor> healthStage2 = sgColors.add(new ColorSetting.Builder()
            .name("health-stage-2")
            .description("The color if a player is at two-thirds health.")
            .defaultValue(new SettingColor(255, 105, 25))
            .build()
    );

    private final Setting<SettingColor> healthStage3 = sgColors.add(new ColorSetting.Builder()
            .name("health-stage-3")
            .description("The color of a player if they are at one-third health.")
            .defaultValue(new SettingColor(255, 25, 25))
            .build()
    );

    private final Setting<SettingColor> enchantmentTextColor = sgColors.add(new ColorSetting.Builder()
            .name("enchantment-text-color")
            .description("The color of the enchantment text.")
            .defaultValue(new SettingColor(255, 255, 255))
            .build()
    );

    public Nametags() {
        super(Category.Render, "nametags", "Displays customizable nametags above players.");
    }

    @EventHandler
    private void onRender(RenderEvent event) {
        for (Entity entity : mc.world.getEntities()) {
            boolean a = !Modules.get().isActive(Freecam.class);
            if (!(entity instanceof PlayerEntity) || (a && entity == mc.player) || (a && entity == mc.cameraEntity)) continue;
            if (!yourself.get() && entity.getUuid().equals(mc.player.getUuid())) continue;

            renderNametag(event, (PlayerEntity) entity);
        }
    }

    private void renderNametag(RenderEvent event, PlayerEntity entity) {
        // Get ping
        int ping;
        try {
            PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(entity.getUuid());
            ping = playerListEntry.getLatency();
        }catch(NullPointerException ignored){
            ping = 0;
        }

        // Compute health things
        float absorption = entity.getAbsorptionAmount();
        int health = Math.round(entity.getHealth() + absorption);
        double healthPercentage = health / (entity.getMaxHealth() + absorption);

        String name;
        if (entity == mc.player && Modules.get().get(NameProtect.class).isActive()) {
            name = Modules.get().get(NameProtect.class).getName(entity.getGameProfile().getName());
        } else if (Modules.get().get(FakePlayer.class).showID(entity)) {
            name = entity.getGameProfile().getName() + " [" + FakePlayerUtils.getID((FakePlayerEntity) entity) + "]";
        } else name = entity.getGameProfile().getName();

        String healthText = " " + health;
        String pingText = " [" + ping + "]";

        NametagUtils.begin(event, entity, scale.get());

        // Get armor info
        double[] armorWidths = new double[4];
        boolean hasArmor = false;
        int maxEnchantCount = 0;
        if (displayArmor.get() || displayArmorEnchants.get()) {
            TextRenderer.get().begin(0.5 * enchantTextScale.get(), true, true);

            for (int i = 0; i < 4; i++) {
                ItemStack itemStack = entity.inventory.armor.get(i);
                Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(itemStack);
                Map<Enchantment, Integer> enchantmentsToShowScale = new HashMap<>();
                for (Enchantment enchantment : displayedEnchantments.get()) {
                    if (enchantments.containsKey(enchantment)) {
                        enchantmentsToShowScale.put(enchantment, enchantments.get(enchantment));
                    }
                }

                if (armorWidths[i] == 0) armorWidths[i] = 16;
                if (!itemStack.isEmpty() && displayArmor.get()) hasArmor = true;

                if (displayArmorEnchants.get()) {
                    for (Enchantment enchantment : enchantmentsToShowScale.keySet()) {
                        String enchantName = Utils.getEnchantShortName(enchantment) + " " + enchantmentsToShowScale.get(enchantment);
                        armorWidths[i] = Math.max(armorWidths[i], TextRenderer.get().getWidth(enchantName));
                    }

                    maxEnchantCount = Math.max(maxEnchantCount, enchantmentsToShowScale.size());
                }
            }

            TextRenderer.get().end();
        }

        // Setup size
        TextRenderer.get().begin();
        double nameWidth = TextRenderer.get().getWidth(name);
        double healthWidth = TextRenderer.get().getWidth(healthText);
        double pingWidth = TextRenderer.get().getWidth(pingText);
        double width = nameWidth + healthWidth;
        if(displayPing.get()){
            width += pingWidth;
        }
        double armorWidth = 0;
        for (double v : armorWidths) armorWidth += v;
        width = Math.max(width, armorWidth);
        double widthHalf = width / 2;

        double heightDown = TextRenderer.get().getHeight();
        double armorHeight = (hasArmor ? 16 : 0);
        armorHeight = Math.max(armorHeight, maxEnchantCount * TextRenderer.get().getHeight() * enchantTextScale.get());
        double heightUp = armorHeight;
        TextRenderer.get().end();

        // Render background
        MB.texture = false;
        MB.begin(null, DrawMode.Triangles, VertexFormats.POSITION_COLOR);
        MB.quad(-widthHalf - 1, -1, 0, -widthHalf - 1, heightDown, 0, widthHalf + 1, heightDown, 0, widthHalf + 1, -1, 0, BACKGROUND);
        MB.end();

        // Render armor
        double itemSpacing = (width - armorWidth) / 4;
        if (hasArmor) {
            double itemX = -widthHalf;
            MB.texture = true;
            MB.begin(null, DrawMode.Triangles, VertexFormats.POSITION_TEXTURE_COLOR);

            boolean isDamaged = false;

            for (int i = 0; i < 4; i++) {
                ItemStack itemStack = entity.inventory.armor.get(i);

                if (itemStack.isDamaged()) isDamaged = true;

                for (BakedQuad quad : mc.getItemRenderer().getModels().getModel(itemStack).getQuads(null, null, null)) {
                    Sprite sprite = ((IBakedQuad) quad).getSprite();

                    if (itemStack.getItem() instanceof DyeableArmorItem) {
                        int c = ((DyeableArmorItem) itemStack.getItem()).getColor(itemStack);

                        WHITE.r = Color.toRGBAR(c);
                        WHITE.g = Color.toRGBAG(c);
                        WHITE.b = Color.toRGBAB(c);
                    }

                    double preItemX = itemX;
                    itemX += (armorWidths[i] - 16) / 2;
                    double addY = (armorHeight - 16) / 2;

                    MB.texQuad(itemX, -heightUp + addY, 16, 16, sprite.getMinU(), sprite.getMinV(), sprite.getMaxU() - sprite.getMinU(), sprite.getMaxV() - sprite.getMinV(), WHITE, WHITE, WHITE, WHITE);

                    itemX = preItemX;
                    WHITE.r = WHITE.g = WHITE.b = 255;
                }

                itemX += armorWidths[i] + itemSpacing;
            }
            mc.getTextureManager().bindTexture(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
            MB.end();

            // Durability
            if (isDamaged) {
                itemX = -widthHalf;
                MB.texture = false;
                MB.begin(null, DrawMode.Triangles, VertexFormats.POSITION_COLOR);

                for (int i = 0; i < 4; i++) {
                    ItemStack itemStack = entity.inventory.armor.get(i);

                    double damage = Math.max(0, itemStack.getDamage());
                    double maxDamage = itemStack.getMaxDamage();
                    double percentage = Math.max(0.0F, (maxDamage - damage) / maxDamage);

                    double j = Math.round(13.0F - damage * 13.0F / maxDamage);
                    int k = MathHelper.hsvToRgb((float) (percentage / 3.0), 1, 1);

                    double preItemX = itemX;
                    itemX += (armorWidths[i] - 17) / 2;
                    double addY = (armorHeight - 16) / 2;

                    WHITE.r = WHITE.g = WHITE.b = 0;
                    MB.quad(itemX + 2, -heightUp + 13 + addY, 0, itemX + 2 + 13, -heightUp + 13 + addY, 0, itemX + 2 + 13, -heightUp + 2 + 13 + addY, 0, itemX + 2, -heightUp + 2 + 13 + addY, 0, WHITE);

                    WHITE.r = k >> 16 & 255;
                    WHITE.g = k >> 8 & 255;
                    WHITE.b = k & 255;
                    MB.quad(itemX + 2, -heightUp + 13 + addY, 0, itemX + 2 + j, -heightUp + 13 + addY, 0, itemX + 2 + j, -heightUp + 1 + 13 + addY, 0, itemX + 2, -heightUp + 1 + 13 + addY, 0, WHITE);

                    WHITE.r = WHITE.g = WHITE.b = 255;
                    itemX = preItemX;

                    itemX += armorWidths[i] + itemSpacing;
                }

                MB.end();
            }
        }

        // Get health color
        Color healthColor;
        if (healthPercentage <= 0.333) healthColor = healthStage3.get();
        else if (healthPercentage <= 0.666) healthColor = healthStage2.get();
        else healthColor = healthStage1.get();

        // Render name, health enchant and texts
        TextRenderer.get().begin(1, false, true);
        Color nameColor = Friends.get().getFriendColor(entity);
        double hX = TextRenderer.get().render(name, -widthHalf, 0, nameColor != null ? nameColor : normalName.get());
        hX = TextRenderer.get().render(healthText, hX, 0, healthColor);
        if (displayPing.get()) TextRenderer.get().render(pingText, hX, 0, pingColor.get());
        double itemX = -widthHalf;
        TextRenderer.get().end();

        if (maxEnchantCount > 0) {
            TextRenderer.get().begin(0.5 * enchantTextScale.get(), false, true);

            for (int i = 0; i < 4; i++) {
                ItemStack itemStack = entity.inventory.armor.get(i);
                Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(itemStack);
                Map<Enchantment, Integer> enchantmentsToShow = new HashMap<>();
                for (Enchantment enchantment : displayedEnchantments.get()) {
                    if (enchantments.containsKey(enchantment)) {
                        enchantmentsToShow.put(enchantment, enchantments.get(enchantment));
                    }
                }

                double aW = armorWidths[i];
                double enchantY = 0;
                double addY = (armorHeight - enchantmentsToShow.size() * TextRenderer.get().getHeight()) / 2;
                if (displayOnItem.get() == Position.Above) {
                    addY -= 16;
                }

                for (Enchantment enchantment : enchantmentsToShow.keySet()) {
                    String enchantName = Utils.getEnchantShortName(enchantment) + " " + enchantmentsToShow.get(enchantment);
                    TextRenderer.get().render(enchantName, itemX + ((aW - TextRenderer.get().getWidth(enchantName)) / 2), -heightUp + enchantY + addY, enchantmentTextColor.get());

                    enchantY += TextRenderer.get().getHeight();
                }

                itemX += armorWidths[i] + itemSpacing;
            }

            TextRenderer.get().end();
        }

        NametagUtils.end();
    }

    private List<Enchantment> setDefualtList(){
        List<Enchantment> ench = new ArrayList<>();
        for (Enchantment enchantment : Registry.ENCHANTMENT) {
            ench.add(enchantment);
        }
        return ench;
    }
}