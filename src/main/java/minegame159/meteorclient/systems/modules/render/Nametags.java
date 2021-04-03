/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render;

//Updated by squidoodly 03/07/2020
//Updated by squidoodly 30/07/2020
//Rewritten (kinda (:troll:)) by snale 07/02/2021

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.render.Render2DEvent;
import minegame159.meteorclient.rendering.DrawMode;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.rendering.text.TextRenderer;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.friends.Friends;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.systems.modules.player.FakePlayer;
import minegame159.meteorclient.systems.modules.player.NameProtect;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.entity.EntityUtils;
import minegame159.meteorclient.utils.entity.FakePlayerEntity;
import minegame159.meteorclient.utils.entity.FakePlayerUtils;
import minegame159.meteorclient.utils.misc.MeteorPlayers;
import minegame159.meteorclient.utils.misc.Vec3;
import minegame159.meteorclient.utils.render.NametagUtils;
import minegame159.meteorclient.utils.render.color.Color;
import minegame159.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.GameMode;

import java.util.*;

import static org.lwjgl.opengl.GL11.*;

public class Nametags extends Module {
    public enum Position {
        Above,
        OnTop
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlayers = settings.createGroup("Players");
    private final SettingGroup sgItems = settings.createGroup("Items");
    private final SettingGroup sgOther = settings.createGroup("Other");

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

    //Players

    private final Setting<Boolean> displayItems = sgPlayers.add(new BoolSetting.Builder()
            .name("display-items")
            .description("Displays armor and hand items above the name tags.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> itemSpacing = sgPlayers.add(new DoubleSetting.Builder()
            .name("item-spacing")
            .description("The spacing between items.")
            .defaultValue(2)
            .min(0)
            .sliderMax(5)
            .max(10)
            .build()
    );

    private final Setting<Boolean> ignoreEmpty = sgPlayers.add(new BoolSetting.Builder()
            .name("ignore-empty")
            .description("Doesn't add spacing where an empty item stack would be.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> displayItemEnchants = sgPlayers.add(new BoolSetting.Builder()
            .name("display-enchants")
            .description("Displays item enchantments on the items.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Position> enchantPos = sgPlayers.add(new EnumSetting.Builder<Position>()
            .name("enchantment-position")
            .description("Where the enchantments are rendered.")
            .defaultValue(Position.Above)
            .build()
    );

    private final Setting<Integer> enchantLength = sgPlayers.add(new IntSetting.Builder()
            .name("enchant-name-length")
            .description("The length enchantment names are trimmed to.")
            .defaultValue(3)
            .min(1)
            .max(5)
            .sliderMax(5)
            .build()
    );

    private final Setting<List<Enchantment>> displayedEnchantments = sgPlayers.add(new EnchListSetting.Builder()
            .name("displayed-enchantments")
            .description("The enchantments that are shown on nametags.")
            .defaultValue(setDefaultList())
            .build()
    );

    private final Setting<Double> enchantTextScale = sgPlayers.add(new DoubleSetting.Builder()
            .name("enchant-text-scale")
            .description("The scale of the enchantment text.")
            .defaultValue(1)
            .min(0.1)
            .max(2)
            .sliderMin(0.1)
            .sliderMax(2)
            .build()
    );

    private final Setting<Boolean> displayMeteor = sgPlayers.add(new BoolSetting.Builder()
            .name("meteor")
            .description("Shows if the player is using Meteor.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> displayGameMode = sgPlayers.add(new BoolSetting.Builder()
            .name("gamemode")
            .description("Shows the player's GameMode.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> displayPing = sgPlayers.add(new BoolSetting.Builder()
            .name("ping")
            .description("Shows the player's ping.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> displayDistance = sgPlayers.add(new BoolSetting.Builder()
            .name("distance")
            .description("Shows the distance between you and the player.")
            .defaultValue(true)
            .build()
    );

    private final Setting<SettingColor> normalName = sgPlayers.add(new ColorSetting.Builder()
            .name("normal-color")
            .description("The color of people not in your Friends List.")
            .defaultValue(new SettingColor(255, 255, 255))
            .build()
    );

    private final Setting<SettingColor> meteorColor = sgPlayers.add(new ColorSetting.Builder()
            .name("meteor-color")
            .description("The color of M when the player is using Meteor.")
            .defaultValue(new SettingColor(135, 0, 255))
            .build()
    );

    private final Setting<SettingColor> gmColor = sgPlayers.add(new ColorSetting.Builder()
            .name("gamemode-color")
            .description("The color of the gamemode text.")
            .defaultValue(new SettingColor(232, 185, 35))
            .build()
    );

    private final Setting<SettingColor> pingColor = sgPlayers.add(new ColorSetting.Builder()
            .name("ping-color")
            .description("The color of the ping text.")
            .defaultValue(new SettingColor(150, 150, 150))
            .build()
    );

    private final Setting<SettingColor> distanceColor = sgPlayers.add(new ColorSetting.Builder()
            .name("distance-color")
            .description("The color of the distance text.")
            .defaultValue(new SettingColor(150, 150, 150))
            .build()
    );

    private final Setting<SettingColor> healthStage1 = sgPlayers.add(new ColorSetting.Builder()
            .name("health-stage-1")
            .description("The color if a player is full health.")
            .defaultValue(new SettingColor(25, 252, 25))
            .build()
    );

    private final Setting<SettingColor> healthStage2 = sgPlayers.add(new ColorSetting.Builder()
            .name("health-stage-2")
            .description("The color if a player is at two-thirds health.")
            .defaultValue(new SettingColor(255, 105, 25))
            .build()
    );

    private final Setting<SettingColor> healthStage3 = sgPlayers.add(new ColorSetting.Builder()
            .name("health-stage-3")
            .description("The color of a player if they are at one-third health.")
            .defaultValue(new SettingColor(255, 25, 25))
            .build()
    );

    private final Setting<SettingColor> enchantmentTextColor = sgPlayers.add(new ColorSetting.Builder()
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
            .name("item-count-color")
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

    private final Vec3 pos = new Vec3();

    private final double[] itemWidths = new double[6];
    private final Color RED = new Color(255, 15, 15);
    private final Map<Enchantment, Integer> enchantmentsToShowScale = new HashMap<>();

    public Nametags() {
        super(Categories.Render, "nametags", "Displays customizable nametags above players.");
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        boolean notFreecamActive = !Modules.get().isActive(Freecam.class);

        for (Entity entity : mc.world.getEntities()) {
            if (!entities.get().containsKey(entity.getType())) continue;
            EntityType<?> type = entity.getType();

            if (type == EntityType.PLAYER) {
                if (notFreecamActive && entity == mc.cameraEntity) continue;
                if (!yourself.get() && entity == mc.player) continue;
            }

            pos.set(entity, event.tickDelta);
            pos.add(0, getHeight(entity), 0);

            if (NametagUtils.to2D(pos, scale.get())) {
                if (type == EntityType.PLAYER) renderNametagPlayer((PlayerEntity) entity);
                else if (type == EntityType.ITEM) renderNametagItem(((ItemEntity) entity).getStack());
                else if (type == EntityType.ITEM_FRAME) renderNametagItem(((ItemFrameEntity) entity).getHeldItemStack());
                else if (type == EntityType.TNT) renderTntNametag((TntEntity) entity);
                else if (entity instanceof LivingEntity) renderGenericNametag((LivingEntity) entity);
            }
        }
    }

    private double getHeight(Entity entity) {
        double height = entity.getEyeHeight(entity.getPose());

        if (entity.getType() == EntityType.ITEM || entity.getType() == EntityType.ITEM_FRAME) height += 0.2;
        else height += 0.5;

        return height;
    }

    private void renderNametagPlayer(PlayerEntity player) {
        TextRenderer text = TextRenderer.get();
        NametagUtils.begin(pos);

        // Using Meteor
        String usingMeteor = "";
        if (displayMeteor.get() && MeteorPlayers.get(player)) usingMeteor = "M ";

        // Gamemode
        GameMode gm = EntityUtils.getGameMode(player);
        String gmText = "ERR";
        if (gm != null) {
            switch (gm) {
                case SPECTATOR: gmText = "Sp"; break;
                case SURVIVAL:  gmText = "S"; break;
                case CREATIVE:  gmText = "C"; break;
                case ADVENTURE: gmText = "A"; break;
            }
        }

        gmText = "[" + gmText + "] ";

        // Name
        String name;
        Color nameColor = Friends.get().getFriendColor(player);

        if (player == mc.player) name = Modules.get().get(NameProtect.class).getName(player.getGameProfile().getName());
        else name = player.getGameProfile().getName();

        if (Modules.get().get(FakePlayer.class).showID(player)) {
            name += " [" + FakePlayerUtils.getID((FakePlayerEntity) player) + "]";
        }
        name = name + " ";

        // Health
        float absorption = player.getAbsorptionAmount();
        int health = Math.round(player.getHealth() + absorption);
        double healthPercentage = health / (player.getMaxHealth() + absorption);

        String healthText = String.valueOf(health);
        Color healthColor;

        if (healthPercentage <= 0.333) healthColor = healthStage3.get();
        else if (healthPercentage <= 0.666) healthColor = healthStage2.get();
        else healthColor = healthStage1.get();

        // Ping
        int ping = EntityUtils.getPing(player);
        String pingText = " [" + ping + "ms]";

        // Distance
        double dist = Math.round(Utils.distanceToCamera(player) * 10.0) / 10.0;
        String distText = " " + dist + "m";

        // Calc widths
        double usingMeteorWidth = text.getWidth(usingMeteor);
        double gmWidth = text.getWidth(gmText);
        double nameWidth = text.getWidth(name);
        double healthWidth = text.getWidth(healthText);
        double pingWidth = text.getWidth(pingText);
        double distWidth = text.getWidth(distText);
        double width = nameWidth + healthWidth;

        if (displayMeteor.get()) width += usingMeteorWidth;
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

        if (displayMeteor.get()) hX = text.render(usingMeteor, hX, hY, meteorColor.get());

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

            for (int i = 0; i < 6; i++) {
                ItemStack itemStack = getItem(player, i);

                // Setting up widths
                if (itemWidths[i] == 0 && (!ignoreEmpty.get() || !itemStack.isEmpty())) itemWidths[i] = 32 + itemSpacing.get();

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
                ItemStack stack = getItem(player, i);

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
        } else if (displayItemEnchants.get()) displayItemEnchants.set(false);

        NametagUtils.end();
    }

    private void renderNametagItem(ItemStack stack) {
        TextRenderer text = TextRenderer.get();
        NametagUtils.begin(pos);

        String name = stack.getName().getString();
        String count = " x" + stack.getCount();

        double nameWidth = text.getWidth(name);
        double countWidth = text.getWidth(count);
        double heightDown = text.getHeight();

        double width = nameWidth;
        if (itemCount.get()) width += countWidth;
        double widthHalf = width / 2;

        drawBg(-widthHalf, -heightDown, width, heightDown);

        text.beginBig();
        double hX = -widthHalf;
        double hY = -heightDown;

        hX = text.render(name, hX, hY, itemNameColor.get());
        if (itemCount.get()) text.render(count, hX, hY, itemCountColor.get());
        text.end();

        NametagUtils.end();
    }

    private void renderGenericNametag(LivingEntity entity) {
        TextRenderer text = TextRenderer.get();
        NametagUtils.begin(pos);

        //Name
        String nameText = entity.getType().getName().getString();
        nameText += " ";

        //Health
        float absorption = entity.getAbsorptionAmount();
        int health = Math.round(entity.getHealth() + absorption);
        double healthPercentage = health / (entity.getMaxHealth() + absorption);

        String healthText = String.valueOf(health);
        Color healthColor;

        if (healthPercentage <= 0.333) healthColor = otherHealthStage3.get();
        else if (healthPercentage <= 0.666) healthColor = otherHealthStage2.get();
        else healthColor = otherHealthStage1.get();

        double nameWidth = text.getWidth(nameText);
        double healthWidth = text.getWidth(healthText);
        double heightDown = text.getHeight();

        double width = nameWidth + healthWidth;
        double widthHalf = width / 2;

        drawBg(-widthHalf, -heightDown, width, heightDown);

        text.beginBig();
        double hX = -widthHalf;
        double hY = -heightDown;

        hX = text.render(nameText, hX, hY, otherNameColor.get());
        text.render(healthText, hX, hY, healthColor);
        text.end();

        NametagUtils.end();
    }

    private void renderTntNametag(TntEntity entity) {
        TextRenderer text = TextRenderer.get();
        NametagUtils.begin(pos);

        String fuseText = ticksToTime(entity.getFuseTimer());

        double width = text.getWidth(fuseText);
        double heightDown = text.getHeight();

        double widthHalf = width / 2;

        drawBg(-widthHalf, -heightDown, width, heightDown);

        text.beginBig();
        double hX = -widthHalf;
        double hY = -heightDown;

        text.render(fuseText, hX, hY, otherNameColor.get());
        text.end();

        NametagUtils.end();
    }

    private List<Enchantment> setDefaultList(){
        List<Enchantment> ench = new ArrayList<>();
        for (Enchantment enchantment : Registry.ENCHANTMENT) {
            ench.add(enchantment);
        }
        return ench;
    }

    private ItemStack getItem(PlayerEntity entity, int index) {
        switch (index) {
            case 0: return entity.getMainHandStack();
            case 1: return entity.inventory.armor.get(3);
            case 2: return entity.inventory.armor.get(2);
            case 3: return entity.inventory.armor.get(1);
            case 4: return entity.inventory.armor.get(0);
            case 5: return entity.getOffHandStack();
        }
        return ItemStack.EMPTY;
    }

    private void drawBg(double x, double y, double width, double height) {
        Renderer.NORMAL.begin(null, DrawMode.Triangles, VertexFormats.POSITION_COLOR);
        Renderer.NORMAL.quad(x - 1, y - 1, width + 2, height + 2, background.get());
        Renderer.NORMAL.end();
    }

    private static String ticksToTime(int ticks){
        if (ticks > 20*3600){
            int h = ticks/20/3600;
            return h+" h";
        } else if (ticks > 20*60){
            int m = ticks/20/60;
            return m+" m";
        } else {
            int s = ticks / 20;
            int ms = (ticks % 20) / 2;
            return s+"."+ms+" s";
        }
    }
}