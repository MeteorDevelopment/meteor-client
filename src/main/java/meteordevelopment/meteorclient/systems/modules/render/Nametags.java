/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.NameProtect;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.*;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.joml.Vector3d;

import java.util.*;

public class Nametags extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlayers = settings.createGroup("Players");
    private final SettingGroup sgItems = settings.createGroup("Items");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Select entities to draw nametags on.")
        .defaultValue(EntityType.PLAYER, EntityType.ITEM)
        .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The scale of the nametag.")
        .defaultValue(1.1)
        .min(0.1)
        .build()
    );

    private final Setting<Boolean> ignoreSelf = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-self")
        .description("Ignore yourself when in third person or freecam.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Ignore rendering nametags for friends.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> ignoreBots = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-bots")
        .description("Only render non-bot nametags.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> culling = sgGeneral.add(new BoolSetting.Builder()
        .name("culling")
        .description("Only render a certain number of nametags at a certain distance.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> maxCullRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("culling-range")
        .description("Only render nametags within this distance of your player.")
        .defaultValue(20)
        .min(0)
        .sliderMax(200)
        .visible(culling::get)
        .build()
    );

    private final Setting<Integer> maxCullCount = sgGeneral.add(new IntSetting.Builder()
        .name("culling-count")
        .description("Only render this many nametags.")
        .defaultValue(50)
        .min(1)
        .sliderRange(1, 100)
        .visible(culling::get)
        .build()
    );

    //Players

    private final Setting<Boolean> displayGameMode = sgPlayers.add(new BoolSetting.Builder()
        .name("gamemode")
        .description("Shows the player's GameMode.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> displayDistance = sgPlayers.add(new BoolSetting.Builder()
        .name("distance")
        .description("Shows the distance between you and the player.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> displayPing = sgPlayers.add(new BoolSetting.Builder()
        .name("ping")
        .description("Shows the player's ping.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> displayItems = sgPlayers.add(new BoolSetting.Builder()
        .name("items")
        .description("Displays armor and hand items above the name tags.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> itemSpacing = sgPlayers.add(new DoubleSetting.Builder()
        .name("item-spacing")
        .description("The spacing between items.")
        .defaultValue(2)
        .range(0, 10)
        .visible(displayItems::get)
        .build()
    );

    private final Setting<Boolean> ignoreEmpty = sgPlayers.add(new BoolSetting.Builder()
        .name("ignore-empty-slots")
        .description("Doesn't add spacing where an empty item stack would be.")
        .defaultValue(true)
        .visible(displayItems::get)
        .build()
    );

    private final Setting<Boolean> displayEnchants = sgPlayers.add(new BoolSetting.Builder()
        .name("display-enchants")
        .description("Displays item enchantments on the items.")
        .defaultValue(false)
        .visible(displayItems::get)
        .build()
    );

    private final Setting<List<Enchantment>> shownEnchantments = sgPlayers.add(new EnchantmentListSetting.Builder()
        .name("shown-enchantments")
        .description("The enchantments that are shown on nametags.")
        .visible(() -> displayItems.get() && displayEnchants.get())
        .defaultValue(
            Enchantments.PROTECTION,
            Enchantments.BLAST_PROTECTION,
            Enchantments.FIRE_PROTECTION,
            Enchantments.PROJECTILE_PROTECTION
        )
        .build()
    );

    private final Setting<Position> enchantPos = sgPlayers.add(new EnumSetting.Builder<Position>()
        .name("enchantment-position")
        .description("Where the enchantments are rendered.")
        .defaultValue(Position.Above)
        .visible(() -> displayItems.get() && displayEnchants.get())
        .build()
    );

    private final Setting<Integer> enchantLength = sgPlayers.add(new IntSetting.Builder()
        .name("enchant-name-length")
        .description("The length enchantment names are trimmed to.")
        .defaultValue(3)
        .range(1, 5)
        .sliderRange(1, 5)
        .visible(() -> displayItems.get() && displayEnchants.get())
        .build()
    );

    private final Setting<Double> enchantTextScale = sgPlayers.add(new DoubleSetting.Builder()
        .name("enchant-text-scale")
        .description("The scale of the enchantment text.")
        .defaultValue(1)
        .range(0.1, 2)
        .sliderRange(0.1, 2)
        .visible(() -> displayItems.get() && displayEnchants.get())
        .build()
    );

    //Items

    private final Setting<Boolean> itemCount = sgItems.add(new BoolSetting.Builder()
        .name("show-count")
        .description("Displays the number of items in the stack.")
        .defaultValue(true)
        .build()
    );

    // Render

    private final Setting<SettingColor> background = sgRender.add(new ColorSetting.Builder()
        .name("background-color")
        .description("The color of the nametag background.")
        .defaultValue(new SettingColor(0, 0, 0, 75))
        .build()
    );

    private final Setting<SettingColor> nameColor = sgRender.add(new ColorSetting.Builder()
        .name("name-color")
        .description("The color of the nametag names.")
        .defaultValue(new SettingColor())
        .build()
    );

    private final Setting<SettingColor> pingColor = sgRender.add(new ColorSetting.Builder()
        .name("ping-color")
        .description("The color of the nametag ping.")
        .defaultValue(new SettingColor(20, 170, 170))
        .visible(displayPing::get)
        .build()
    );

    private final Setting<SettingColor> gamemodeColor = sgRender.add(new ColorSetting.Builder()
        .name("gamemode-color")
        .description("The color of the nametag gamemode.")
        .defaultValue(new SettingColor(232, 185, 35))
        .visible(displayGameMode::get)
        .build()
    );

    private final Setting<DistanceColorMode> distanceColorMode = sgRender.add(new EnumSetting.Builder<DistanceColorMode>()
        .name("distance-color-mode")
        .description("The mode to color the nametag distance with.")
        .defaultValue(DistanceColorMode.Gradient)
        .visible(displayDistance::get)
        .build()
    );

    private final Setting<SettingColor> distanceColor = sgRender.add(new ColorSetting.Builder()
        .name("distance-color")
        .description("The color of the nametag distance.")
        .defaultValue(new SettingColor(150, 150, 150))
        .visible(() -> displayDistance.get() && distanceColorMode.get() == DistanceColorMode.Flat)
        .build()
    );

    private final Color WHITE = new Color(255, 255, 255);
    private final Color RED = new Color(255, 25, 25);
    private final Color AMBER = new Color(255, 105, 25);
    private final Color GREEN = new Color(25, 252, 25);
    private final Color GOLD = new Color(232, 185, 35);

    private final Vector3d pos = new Vector3d();
    private final double[] itemWidths = new double[6];

    private final List<Entity> entityList = new ArrayList<>();

    public Nametags() {
        super(Categories.Render, "nametags", "Displays customizable nametags above players.");
    }

    private static String ticksToTime(int ticks) {
        if (ticks > 20 * 3600) {
            int h = ticks / 20 / 3600;
            return h + " h";
        } else if (ticks > 20 * 60) {
            int m = ticks / 20 / 60;
            return m + " m";
        } else {
            int s = ticks / 20;
            int ms = (ticks % 20) / 2;
            return s + "." + ms + " s";
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        entityList.clear();

        boolean freecamNotActive = !Modules.get().isActive(Freecam.class);
        boolean notThirdPerson = mc.options.getPerspective().isFirstPerson();
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();

        for (Entity entity : mc.world.getEntities()) {
            EntityType<?> type = entity.getType();
            if (!entities.get().containsKey(type)) continue;

            if (type == EntityType.PLAYER) {
                if ((ignoreSelf.get() || (freecamNotActive && notThirdPerson)) && entity == mc.player) continue;
                if (EntityUtils.getGameMode((PlayerEntity) entity) == null && ignoreBots.get()) continue;
                if (Friends.get().isFriend((PlayerEntity) entity) && ignoreFriends.get()) continue;
            }

            if (!culling.get() || PlayerUtils.isWithinCamera(entity, maxCullRange.get())) {
                entityList.add(entity);
            }
        }

        entityList.sort(Comparator.comparing(e -> e.squaredDistanceTo(cameraPos)));
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        int count = getRenderCount();
        boolean shadow = Config.get().customFont.get();

        for (int i = count - 1; i > -1; i--) {
            Entity entity = entityList.get(i);

            Utils.set(pos, entity, event.tickDelta);
            pos.add(0, getHeight(entity), 0);

            EntityType<?> type = entity.getType();

            if (NametagUtils.to2D(pos, scale.get())) {
                if (type == EntityType.PLAYER) renderNametagPlayer((PlayerEntity) entity, shadow);
                else if (type == EntityType.ITEM) renderNametagItem(((ItemEntity) entity).getStack(), shadow);
                else if (type == EntityType.ITEM_FRAME)
                    renderNametagItem(((ItemFrameEntity) entity).getHeldItemStack(), shadow);
                else if (type == EntityType.TNT) renderTntNametag((TntEntity) entity, shadow);
                else if (entity instanceof LivingEntity) renderGenericNametag((LivingEntity) entity, shadow);
            }
        }
    }

    private int getRenderCount() {
        int count = culling.get() ? maxCullCount.get() : entityList.size();
        count = MathHelper.clamp(count, 0, entityList.size());

        return count;
    }

    @Override
    public String getInfoString() {
        return Integer.toString(getRenderCount());
    }

    private double getHeight(Entity entity) {
        double height = entity.getEyeHeight(entity.getPose());

        if (entity.getType() == EntityType.ITEM || entity.getType() == EntityType.ITEM_FRAME) height += 0.2;
        else height += 0.5;

        return height;
    }

    private void renderNametagPlayer(PlayerEntity player, boolean shadow) {
        TextRenderer text = TextRenderer.get();
        NametagUtils.begin(pos);

        // Gamemode
        GameMode gm = EntityUtils.getGameMode(player);
        String gmText = "BOT";
        if (gm != null) {
            gmText = switch (gm) {
                case SPECTATOR -> "Sp";
                case SURVIVAL -> "S";
                case CREATIVE -> "C";
                case ADVENTURE -> "A";
            };
        }

        gmText = "[" + gmText + "] ";

        // Name
        String name;
        Color nameColor = PlayerUtils.getPlayerColor(player, this.nameColor.get());

        if (player == mc.player) name = Modules.get().get(NameProtect.class).getName(player.getEntityName());
        else name = player.getEntityName();

        name = name + " ";

        // Health
        float absorption = player.getAbsorptionAmount();
        int health = Math.round(player.getHealth() + absorption);
        double healthPercentage = health / (player.getMaxHealth() + absorption);

        String healthText = String.valueOf(health);
        Color healthColor;

        if (healthPercentage <= 0.333) healthColor = RED;
        else if (healthPercentage <= 0.666) healthColor = AMBER;
        else healthColor = GREEN;

        // Ping
        int ping = EntityUtils.getPing(player);
        String pingText = " [" + ping + "ms]";

        // Distance
        double dist = Math.round(PlayerUtils.distanceToCamera(player) * 10.0) / 10.0;
        String distText = " " + dist + "m";

        // Calc widths
        double gmWidth = text.getWidth(gmText, shadow);
        double nameWidth = text.getWidth(name, shadow);
        double healthWidth = text.getWidth(healthText, shadow);
        double pingWidth = text.getWidth(pingText, shadow);
        double distWidth = text.getWidth(distText, shadow);
        double width = nameWidth + healthWidth;

        boolean renderPlayerDistance = player != mc.cameraEntity || Modules.get().isActive(Freecam.class);

        if (displayGameMode.get()) width += gmWidth;
        if (displayPing.get()) width += pingWidth;
        if (displayDistance.get() && renderPlayerDistance) width += distWidth;

        double widthHalf = width / 2;
        double heightDown = text.getHeight(shadow);

        drawBg(-widthHalf, -heightDown, width, heightDown);

        // Render texts
        text.beginBig();
        double hX = -widthHalf;
        double hY = -heightDown;

        if (displayGameMode.get()) hX = text.render(gmText, hX, hY, gamemodeColor.get(), shadow);
        hX = text.render(name, hX, hY, nameColor, shadow);

        hX = text.render(healthText, hX, hY, healthColor, shadow);
        if (displayPing.get()) hX = text.render(pingText, hX, hY, pingColor.get(), shadow);
        if (displayDistance.get() && renderPlayerDistance) {
            switch (distanceColorMode.get()) {
                case Flat ->  text.render(distText, hX, hY, distanceColor.get(), shadow);
                case Gradient -> text.render(distText, hX, hY, EntityUtils.getColorFromDistance(player), shadow);
            }
        }

        text.end();

        if (displayItems.get()) {
            // Item calc
            Arrays.fill(itemWidths, 0);
            boolean hasItems = false;
            int maxEnchantCount = 0;

            for (int i = 0; i < 6; i++) {
                ItemStack itemStack = getItem(player, i);

                // Setting up widths
                if (itemWidths[i] == 0 && (!ignoreEmpty.get() || !itemStack.isEmpty()))
                    itemWidths[i] = 32 + itemSpacing.get();

                if (!itemStack.isEmpty()) hasItems = true;

                if (displayEnchants.get()) {
                    Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(itemStack);

                    int size = 0;
                    for (var enchantment : enchantments.keySet()) {
                        if (!shownEnchantments.get().contains(enchantment)) continue;
                        String enchantName = Utils.getEnchantSimpleName(enchantment, enchantLength.get()) + " " + enchantments.get(enchantment);
                        itemWidths[i] = Math.max(itemWidths[i], (text.getWidth(enchantName, shadow) / 2));
                        size++;
                    }

                    maxEnchantCount = Math.max(maxEnchantCount, size);
                }
            }

            double itemsHeight = (hasItems ? 32 : 0);
            double itemWidthTotal = 0;
            for (double w : itemWidths) itemWidthTotal += w;
            double itemWidthHalf = itemWidthTotal / 2;

            double y = -heightDown - 7 - itemsHeight;
            double x = -itemWidthHalf;

            // Rendering items and enchants
            for (int i = 0; i < 6; i++) {
                ItemStack stack = getItem(player, i);

                RenderUtils.drawItem(stack, (int) x, (int) y, 2, true);

                if (maxEnchantCount > 0 && displayEnchants.get()) {
                    text.begin(0.5 * enchantTextScale.get(), false, true);

                    Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(stack);
                    Map<Enchantment, Integer> enchantmentsToShow = new HashMap<>();

                    for (Enchantment enchantment : enchantments.keySet()) {
                        if (shownEnchantments.get().contains(enchantment)) {
                            enchantmentsToShow.put(enchantment, enchantments.get(enchantment));
                        }
                    }

                    double aW = itemWidths[i];
                    double enchantY = 0;

                    double addY = switch (enchantPos.get()) {
                        case Above -> -((enchantmentsToShow.size() + 1) * text.getHeight(shadow));
                        case OnTop -> (itemsHeight - enchantmentsToShow.size() * text.getHeight(shadow)) / 2;
                    };

                    double enchantX;

                    for (Enchantment enchantment : enchantmentsToShow.keySet()) {
                        String enchantName = Utils.getEnchantSimpleName(enchantment, enchantLength.get()) + " " + enchantmentsToShow.get(enchantment);

                        Color enchantColor = WHITE;
                        if (enchantment.isCursed()) enchantColor = RED;

                        enchantX = switch (enchantPos.get()) {
                            case Above -> x + (aW / 2) - (text.getWidth(enchantName, shadow) / 2);
                            case OnTop -> x + (aW - text.getWidth(enchantName, shadow)) / 2;
                        };

                        text.render(enchantName, enchantX, y + addY + enchantY, enchantColor, shadow);

                        enchantY += text.getHeight(shadow);
                    }

                    text.end();
                }

                x += itemWidths[i];
            }
        } else if (displayEnchants.get()) displayEnchants.set(false);

        NametagUtils.end();
    }

    private void renderNametagItem(ItemStack stack, boolean shadow) {
        TextRenderer text = TextRenderer.get();
        NametagUtils.begin(pos);

        String name = stack.getName().getString();
        String count = " x" + stack.getCount();

        double nameWidth = text.getWidth(name, shadow);
        double countWidth = text.getWidth(count, shadow);
        double heightDown = text.getHeight(shadow);

        double width = nameWidth;
        if (itemCount.get()) width += countWidth;
        double widthHalf = width / 2;

        drawBg(-widthHalf, -heightDown, width, heightDown);

        text.beginBig();
        double hX = -widthHalf;
        double hY = -heightDown;

        hX = text.render(name, hX, hY, nameColor.get(), shadow);
        if (itemCount.get()) text.render(count, hX, hY, GOLD, shadow);
        text.end();

        NametagUtils.end();
    }

    private void renderGenericNametag(LivingEntity entity, boolean shadow) {
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

        if (healthPercentage <= 0.333) healthColor = RED;
        else if (healthPercentage <= 0.666) healthColor = AMBER;
        else healthColor = GREEN;

        double nameWidth = text.getWidth(nameText, shadow);
        double healthWidth = text.getWidth(healthText, shadow);
        double heightDown = text.getHeight(shadow);

        double width = nameWidth + healthWidth;
        double widthHalf = width / 2;

        drawBg(-widthHalf, -heightDown, width, heightDown);

        text.beginBig();
        double hX = -widthHalf;
        double hY = -heightDown;

        hX = text.render(nameText, hX, hY, nameColor.get(), shadow);
        text.render(healthText, hX, hY, healthColor, shadow);
        text.end();

        NametagUtils.end();
    }

    private void renderTntNametag(TntEntity entity, boolean shadow) {
        TextRenderer text = TextRenderer.get();
        NametagUtils.begin(pos);

        String fuseText = ticksToTime(entity.getFuse());

        double width = text.getWidth(fuseText, shadow);
        double heightDown = text.getHeight(shadow);

        double widthHalf = width / 2;

        drawBg(-widthHalf, -heightDown, width, heightDown);

        text.beginBig();
        double hX = -widthHalf;
        double hY = -heightDown;

        text.render(fuseText, hX, hY, nameColor.get(), shadow);
        text.end();

        NametagUtils.end();
    }

    private ItemStack getItem(PlayerEntity entity, int index) {
        return switch (index) {
            case 0 -> entity.getMainHandStack();
            case 1 -> entity.getInventory().armor.get(3);
            case 2 -> entity.getInventory().armor.get(2);
            case 3 -> entity.getInventory().armor.get(1);
            case 4 -> entity.getInventory().armor.get(0);
            case 5 -> entity.getOffHandStack();
            default -> ItemStack.EMPTY;
        };
    }

    private void drawBg(double x, double y, double width, double height) {
        Renderer2D.COLOR.begin();
        Renderer2D.COLOR.quad(x - 1, y - 1, width + 2, height + 2, background.get());
        Renderer2D.COLOR.render(null);
    }

    public enum Position {
        Above,
        OnTop
    }

    public enum DistanceColorMode {
        Gradient,
        Flat;
    }

    public boolean excludeBots() {
        return ignoreBots.get();
    }

    public boolean playerNametags() {
        return isActive() && entities.get().containsKey(EntityType.PLAYER);
    }
}
