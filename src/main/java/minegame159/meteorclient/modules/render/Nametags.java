/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.render;

//Updated by squidoodly 03/07/2020
//Updated by squidoodly 30/07/2020

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.render.Render2DEvent;
import minegame159.meteorclient.friends.Friends;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.modules.player.FakePlayer;
import minegame159.meteorclient.modules.player.NameProtect;
import minegame159.meteorclient.rendering.DrawMode;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.rendering.text.TextRenderer;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.entity.EntityUtils;
import minegame159.meteorclient.utils.entity.FakePlayerEntity;
import minegame159.meteorclient.utils.entity.FakePlayerUtils;
import minegame159.meteorclient.utils.render.NametagUtils;
import minegame159.meteorclient.utils.render.color.Color;
import minegame159.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.GameMode;

import java.util.*;

import static org.lwjgl.opengl.GL11.*;

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

    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Select entities to draw nametags on.")
            .defaultValue(Utils.asObject2BooleanOpenHashMap(EntityType.PLAYER, EntityType.ITEM))
            .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale")
            .description("The scale of the nametag.")
            .defaultValue(1.5)
            .min(0.1)
            .build()
    );

    private final Setting<Boolean> yourself = sgGeneral.add(new BoolSetting.Builder()
            .name("self-nametag")
            .description("Displays a nametag on your player if you're in Freecam.")
            .defaultValue(true)
            .build()
    );

    private final Setting<SettingColor> background = sgGeneral.add(new ColorSetting.Builder()
            .name("background")
            .description("The color of the nametag background.")
            .defaultValue(new SettingColor(0, 0, 0, 75))
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
            .defaultValue(Position.Above)
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
            .defaultValue(1)
            .min(0.1)
            .max(2)
            .sliderMin(0.1)
            .sliderMax(2)
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

    //Items

    private final Setting<SettingColor> itemNameColor = sgItems.add(new ColorSetting.Builder()
            .name("name-color")
            .description("The color of the name of the item.")
            .defaultValue(new SettingColor(255, 255, 255))
            .build()
    );

    private final Setting<Boolean> itemCount = sgItems.add(new BoolSetting.Builder()
            .name("count-on-items")
            .description("Shows the number of items in an item entities nametag.")
            .defaultValue(true)
            .build()
    );

    private final Setting<SettingColor> itemCountColor = sgItems.add(new ColorSetting.Builder()
            .name("itme-count-color")
            .description("The color of the item count.")
            .defaultValue(new SettingColor(232, 185, 35))
            .build()
    );

    //Other

    private final Setting<SettingColor> otherNameColor = sgOther.add(new ColorSetting.Builder()
            .name("name-color")
            .description("The color of the name of the entity.")
            .defaultValue(new SettingColor(255, 255, 255))
            .build()
    );

    private final Setting<SettingColor> otherHealthStage1 = sgOther.add(new ColorSetting.Builder()
            .name("health-stage-1")
            .description("The color of a mobs health if it's full health.")
            .defaultValue(new SettingColor(25, 252, 25))
            .build()
    );

    private final Setting<SettingColor> otherHealthStage2 = sgOther.add(new ColorSetting.Builder()
            .name("health-stage-2")
            .description("The color of a mobs health if it's at two-thirds health.")
            .defaultValue(new SettingColor(255, 105, 25))
            .build()
    );

    private final Setting<SettingColor> otherHealthStage3 = sgOther.add(new ColorSetting.Builder()
            .name("health-stage-3")
            .description("The color of a mobs health if they are at one-third health.")
            .defaultValue(new SettingColor(255, 25, 25))
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

        // Ping
        int ping = EntityUtils.getPing(entity);
        String pingText = " [" + ping + "ms]";

        // Distance
        double dist = Math.round(Utils.distanceToCamera(entity) * 10.0) / 10.0;
        String distText = " " + dist + "m";

        // Calc widths
        double gmWidth = text.getWidth(gmText);
        double nameWidth = text.getWidth(name);
        double healthWidth = text.getWidth(healthText);
        double pingWidth = text.getWidth(pingText);
        double distWidth = text.getWidth(distText);
        double width = nameWidth + healthWidth;

        if (displayGameMode.get()) width += gmWidth;
        if (displayPing.get()) width += pingWidth;
        if (displayDistance.get()) width += distWidth;

        double widthHalf = width / 2;
        double heightDown = text.getHeight();

        drawBg(-widthHalf, -heightDown, width, heightDown);

        // Render texts
        text.beginBig();
        double hX = -widthHalf;
        double hY = -heightDown;

        if (displayGameMode.get()) hX = text.render(gmText, hX, hY, gmColor.get());
        hX = text.render(name, hX, hY, nameColor != null ? nameColor : normalName.get());

        hX = text.render(healthText, hX, hY, healthColor);
        if (displayPing.get()) hX = text.render(pingText, hX, hY, pingColor.get());
        if (displayDistance.get()) text.render(distText, hX, hY, distanceColor.get());
        text.end();

        if (displayItems.get()) {
            // Item calc
            Arrays.fill(itemWidths, 0);
            boolean hasItems = false;
            int maxEnchantCount = 0;
            if (!displayItems.get()) displayItemEnchants.set(false);

            for (int i = 0; i < 6; i++) {
                ItemStack itemStack = getItem(entity, i);

                // Setting up widths
                if (itemWidths[i] == 0) itemWidths[i] = 32;

                if (!itemStack.isEmpty()) hasItems = true;

                if (displayItemEnchants.get()) {
                    Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(itemStack);
                    enchantmentsToShowScale.clear();

                    for (Enchantment enchantment : displayedEnchantments.get()) {
                        if (enchantments.containsKey(enchantment)) {
                            enchantmentsToShowScale.put(enchantment, enchantments.get(enchantment));
                        }
                    }

                    for (Enchantment enchantment : enchantmentsToShowScale.keySet()) {
                        String enchantName = Utils.getEnchantSimpleName(enchantment, enchantLength.get()) + " " + enchantmentsToShowScale.get(enchantment);
                        itemWidths[i] = Math.max(itemWidths[i], (text.getWidth(enchantName) / 2));
                    }

                    maxEnchantCount = Math.max(maxEnchantCount, enchantmentsToShowScale.size());
                }
            }

            double itemsHeight = (hasItems ? 32 : 0);
            double itemWidthTotal = 0;
            for (double w : itemWidths) itemWidthTotal += w;
            double itemWidthHalf = itemWidthTotal / 2;

            double y = -heightDown - 7 - itemsHeight;
            double x = -itemWidthHalf;

            //Rendering items and enchants
            for (int i = 0; i < 6; i++) {
                ItemStack stack = getItem(entity, i);

                glPushMatrix();
                glScaled(2, 2, 1);

                mc.getItemRenderer().renderGuiItemIcon(stack, (int) (x / 2), (int) (y / 2));
                mc.getItemRenderer().renderGuiItemOverlay(mc.textRenderer, stack, (int) (x / 2), (int) (y / 2));

                glPopMatrix();

                if (maxEnchantCount > 0 && displayItemEnchants.get()) {
                    text.begin(0.5 * enchantTextScale.get(), false, true);

                    Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(stack);
                    Map<Enchantment, Integer> enchantmentsToShow = new HashMap<>();
                    for (Enchantment enchantment : displayedEnchantments.get()) {
                        if (enchantments.containsKey(enchantment)) {
                            enchantmentsToShow.put(enchantment, enchantments.get(enchantment));
                        }
                    }

                    double aW = itemWidths[i];
                    double enchantY = 0;

                    double addY = 0;
                    switch (enchantPos.get()) {
                        case Above: addY = -((enchantmentsToShow.size() + 1) * text.getHeight()); break;
                        case OnTop: addY = (itemsHeight - enchantmentsToShow.size() * text.getHeight()) / 2; break;
                    }

                    double enchantX = x;

                    for (Enchantment enchantment : enchantmentsToShow.keySet()) {
                        String enchantName = Utils.getEnchantSimpleName(enchantment, enchantLength.get()) + " " + enchantmentsToShow.get(enchantment);

                        Color enchantColor = enchantmentTextColor.get();
                        if (enchantment.isCursed()) enchantColor = RED;

                        switch (enchantPos.get()) {
                            case Above: enchantX = x + (aW / 2) - (text.getWidth(enchantName) / 2); break;
                            case OnTop: enchantX = x + (aW - text.getWidth(enchantName)) / 2; break;
                        }

                        text.render(enchantName, enchantX, y + addY + enchantY, enchantColor);

                        enchantY += text.getHeight();
                    }

                    text.end();
                }

                x += itemWidths[i];
            }
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