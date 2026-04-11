/*
 * This file is part of Meteor Client.
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
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
import meteordevelopment.meteorclient.systems.modules.player.NameProtect;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.scoreboard.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;
import org.joml.Vector3d;

import java.util.*;

public class Nametags extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlayers = settings.createGroup("Players");
    private final SettingGroup sgItems   = settings.createGroup("Items");
    private final SettingGroup sgRender  = settings.createGroup("Render");

    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .defaultValue(EntityType.PLAYER, EntityType.ITEM)
        .build());

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale").defaultValue(1.1).min(0.1).build());

    private final Setting<Double> nearDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("near-distance")
        .defaultValue(2.0).min(0.1).max(100.0).sliderRange(0.1, 10.0)
        .build());

    private final Setting<Double> farDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("far-distance")
        .defaultValue(30.0).min(0.1).max(200.0).sliderRange(1.0, 50.0)
        .build());

    private final Setting<Boolean> vanillaOverhead = sgGeneral.add(new BoolSetting.Builder()
        .name("vanilla-overhead")
        .defaultValue(true)
        .build());

    public final Setting<Boolean> hideVanilla = sgGeneral.add(new BoolSetting.Builder()
        .name("hide-vanilla")
        .defaultValue(false)
        .build());

    private final Setting<Boolean> ignoreSelf = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-self").defaultValue(true).build());

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends").defaultValue(false).build());

    private final Setting<Boolean> ignoreBots = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-bots").defaultValue(true).build());

    private final Setting<Integer> maxDistance = sgGeneral.add(new IntSetting.Builder()
        .name("max-distance")
        .defaultValue(100).min(1).max(1000).sliderRange(1, 200)
        .build());

    private final Setting<Boolean> displayHealth = sgPlayers.add(new BoolSetting.Builder()
        .name("health").defaultValue(true).build());

    // ==================== 血量来源模式 ====================
    public enum HealthSource {
        Tab,
        Client
    }

    public final Setting<HealthSource> healthSource = sgPlayers.add(new EnumSetting.Builder<HealthSource>()
        .name("health-source")
        .description("Tab: 从计分板读取真实血量(防作弊绕过)。Client: 本地可见血量。")
        .defaultValue(HealthSource.Tab)
        .build()
    );
    // ====================================================

    private final Setting<Boolean> displayGameMode = sgPlayers.add(new BoolSetting.Builder()
        .name("gamemode").defaultValue(false).build());

    private final Setting<Boolean> displayDistance = sgPlayers.add(new BoolSetting.Builder()
        .name("distance").defaultValue(false).build());

    private final Setting<Boolean> displayPing = sgPlayers.add(new BoolSetting.Builder()
        .name("ping").defaultValue(true).build());

    // Fix #3: espHealthColors 现在实际控制名字颜色随 HP 变化
    private final Setting<Boolean> espHealthColors = sgPlayers.add(new BoolSetting.Builder()
        .name("esp-health-colors")
        .description("根据生命值动态改变名字颜色，<=20橙色，<10红色")
        .defaultValue(true)
        .build());

    private final Setting<Integer> maxNameLength = sgPlayers.add(new IntSetting.Builder()
        .name("max-name-length")
        .defaultValue(10).min(0).max(50)
        .build());

    private final Setting<Boolean> displayItems = sgPlayers.add(new BoolSetting.Builder()
        .name("items").defaultValue(true).build());

    private final Setting<Boolean> displayEnchants = sgPlayers.add(new BoolSetting.Builder()
        .name("display-enchants")
        .defaultValue(true)
        .visible(displayItems::get)
        .build());

    private final Setting<Boolean> displayTeamIcon = sgPlayers.add(new BoolSetting.Builder()
        .name("team-icon")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> displayDamageReduction = sgItems.add(new BoolSetting.Builder()
        .name("damage-reduction")
        .defaultValue(true)
        .build());

    private final Setting<Integer> drMinPercent = sgItems.add(new IntSetting.Builder()
        .name("dr-min-percent")
        .defaultValue(10).min(0).max(100)
        .visible(displayDamageReduction::get)
        .build());

    private final Setting<Boolean> itemCount = sgItems.add(new BoolSetting.Builder()
        .name("show-count").defaultValue(true).build());

    private final Setting<SettingColor> background = sgRender.add(new ColorSetting.Builder()
        .name("background-color")
        .defaultValue(new SettingColor(0, 0, 0, 75))
        .build());

    private final Setting<SettingColor> nameColor = sgRender.add(new ColorSetting.Builder()
        .name("name-color")
        .defaultValue(new SettingColor())
        .build());

    private final Setting<SettingColor> pingColor = sgRender.add(new ColorSetting.Builder()
        .name("ping-color")
        .defaultValue(new SettingColor(20, 170, 170))
        .visible(displayPing::get)
        .build());

    private final Setting<SettingColor> gamemodeColor = sgRender.add(new ColorSetting.Builder()
        .name("gamemode-color")
        .defaultValue(new SettingColor(232, 185, 35))
        .visible(displayGameMode::get)
        .build());

    // 颜色常量
    private final Color LOW_HP    = new Color(200, 80,  80);
    private final Color MEDIUM_HP = new Color(255, 180, 60);
    private final Color HIGH_HP   = new Color(100, 200, 100);
    private final Color WHITE     = new Color(255, 255, 255);
    // Fix #1: 血量固定黑色
    private final Color BLACK     = new Color(0, 0, 0, 255);

    // Fix #4: 只保留一个 mutableColor，renderTeamIcon 改为直接构造 packed int
    private final Color mutableColor = new Color();

    private final Vector3d pos = new Vector3d();
    private final List<Entity> entitiesList = new ArrayList<>();
    private final StringBuilder sb = new StringBuilder();

    private int bgAlpha;
    private int textAlpha;

    // FastUtil 避免装箱开销
    private final Object2IntMap<UUID> tabHealthCache = new Object2IntOpenHashMap<>();

    public Nametags() {
        super(Categories.Render, "nametags", "Nametags");
    }

    @Override
    public void onDeactivate() {
        tabHealthCache.clear();
    }

    // ==================== Tick ====================

    @EventHandler
    private void onTick(TickEvent.Post event) {
        // Tab血量缓存刷新逻辑（同ESP统一）
        tabHealthCache.clear();
        if (healthSource.get() == HealthSource.Tab && mc.world != null) {
            Scoreboard sb = mc.world.getScoreboard();
            ScoreboardObjective obj = sb.getObjectiveForSlot(ScoreboardDisplaySlot.LIST);
            if (obj == null) obj = sb.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);

            for (PlayerEntity player : mc.world.getPlayers()) {
                if (obj != null) {
                    try {
                        ScoreHolder holder = ScoreHolder.fromProfile(player.getGameProfile());
                        ReadableScoreboardScore score = sb.getScore(holder, obj);
                        if (score != null && score.getScore() > 0) {
                            tabHealthCache.put(player.getUuid(), score.getScore());
                            continue; // 成功获取到Tab计分板血量，直接跳过Fallback
                        }
                    } catch (Exception ignored) {}
                }
                // Fallback 到客户端血量
                tabHealthCache.put(player.getUuid(), (int)(player.getHealth() + player.getAbsorptionAmount()));
            }
        }

        // 实体更新逻辑
        entitiesList.clear();
        if (mc.world == null) return;

        boolean freecam     = Modules.get().isActive(Freecam.class);
        boolean firstPerson = mc.options.getPerspective().isFirstPerson();
        double maxDistSq    = (double) maxDistance.get() * maxDistance.get();

        Set<EntityType<?>> entityTypes    = entities.get();
        boolean ignoreSelfFlag    = ignoreSelf.get();
        boolean ignoreFriendsFlag = ignoreFriends.get();
        boolean ignoreBotsFlag    = ignoreBots.get();

        for (Entity entity : mc.world.getEntities()) {
            if (!entityTypes.contains(entity.getType())) continue;
            if (PlayerUtils.squaredDistanceToCamera(entity) > maxDistSq) continue;

            if (entity instanceof PlayerEntity p) {
                if (entity == mc.player && ignoreSelfFlag && !freecam && firstPerson) continue;
                if (ignoreFriendsFlag && Friends.get().isFriend(p)) continue;
                if (ignoreBotsFlag && EntityUtils.getGameMode(p) == null) continue;
            }
            entitiesList.add(entity);
        }

        entitiesList.sort((e1, e2) -> Double.compare(
            PlayerUtils.squaredDistanceToCamera(e2),
            PlayerUtils.squaredDistanceToCamera(e1)
        ));
    }

    // ==================== 血量获取 ====================

    private double getHp(PlayerEntity p) {
        if (healthSource.get() == HealthSource.Tab) {
            UUID uuid = p.getUuid();
            if (tabHealthCache.containsKey(uuid)) {
                return tabHealthCache.getInt(uuid);
            }
        }
        return p.getHealth() + p.getAbsorptionAmount();
    }

    // ==================== Render ====================

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        boolean shadow = true;

        for (Entity entity : entitiesList) {
            Utils.set(pos, entity, event.tickDelta);
            pos.add(0, entity.getEyeHeight(entity.getPose()) + 0.5, 0);

            double dist      = PlayerUtils.distanceToCamera(entity);
            float distFade   = smoothstep(nearDistance.get().floatValue(), farDistance.get().floatValue(), (float) dist);
            float finalScale = MathHelper.clamp((float) (scale.get() * (1 - distFade * 0.5f)), 0.5f, 10f);

            bgAlpha   = (int) MathHelper.lerp(distFade, 255, 60);
            textAlpha = bgAlpha;

            if (!NametagUtils.to2D(pos, finalScale)) continue;

            if (entity instanceof PlayerEntity player) {
                renderPlayer(event, player, shadow, dist);
            } else if (entity instanceof ItemEntity item) {
                renderItem(item.getStack(), shadow);
            }
        }
    }

    // ==================== Player ====================

    private void renderPlayer(Render2DEvent event, PlayerEntity player, boolean shadow, double dist) {
        TextRenderer text = TextRenderer.get();
        NametagUtils.begin(pos);

        // --- 数据准备 ---
        String gmText = "";
        if (displayGameMode.get()) {
            GameMode gm = EntityUtils.getGameMode(player);
            gmText = gm == null ? "[BOT] " : switch (gm) {
                case SPECTATOR -> "[Sp] ";
                case SURVIVAL  -> "[S] ";
                case CREATIVE  -> "[C] ";
                case ADVENTURE -> "[A] ";
            };
        }

        String rawName = player.getName().getString();
        String name = player == mc.player
            ? Modules.get().get(NameProtect.class).getName(rawName)
            : rawName;
        if (maxNameLength.get() > 0 && name.length() > maxNameLength.get()) {
            name = name.substring(0, maxNameLength.get()) + "...";
        } else if (maxNameLength.get() == 0) {
            name = "";
        }

        // --- 核心修复区：统一获取真实血量 ---
        double hpVal = getHp(player);

        // Fix #3: espHealthColors 实际控制名字颜色
        Color nameCol = PlayerUtils.getPlayerColor(player, nameColor.get());
        if (espHealthColors.get()) {
            // 这里统一使用了反作弊绕过获取到的 hpVal，而非只用客户端假血
            if (hpVal < 10)       nameCol = LOW_HP;
            else if (hpVal <= 20) nameCol = MEDIUM_HP;
            else                  nameCol = HIGH_HP;
        }

        // Fix #1: 血量固定黑色
        Color healthCol = BLACK;

        String pingText = "";
        if (displayPing.get()) pingText = " [" + EntityUtils.getPing(player) + "ms]";

        String distText = "";
        if (displayDistance.get()) distText = " " + String.format("%.1f", dist) + "m";

        String drText = "";
        if (displayDamageReduction.get()) {
            float base    = 10f;
            float reduced = DamageUtils.calculateReductions(base, player, mc.world.getDamageSources().playerAttack(mc.player));
            float pct     = MathHelper.clamp(1f - reduced / base, 0f, 1f) * 100f;
            if (pct >= drMinPercent.get()) drText = " [DR " + (int) pct + "%]";
        }

        double height = text.getHeight(shadow);

        // 添加空格前缀，避免与名字粘连
        String healthText = displayHealth.get() ? " " + (int) hpVal : "";

        Team playerTeam = player.getScoreboardTeam();

        if (vanillaOverhead.get()) {
            sb.setLength(0);
            if (displayGameMode.get()) sb.append(gmText);
            appendTeamText(sb, playerTeam);
            sb.append(name).append(healthText).append(pingText).append(distText).append(drText);
            String fullStr = sb.toString();

            double width = text.getWidth(fullStr, shadow);
            double x     = -width / 2;
            double y     = -height * 2;

            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.quad(x - 1, y - 1, width + 2, height + 2,
                newColor(background.get().getPacked(), bgAlpha));
            Renderer2D.COLOR.render();

            text.beginBig();
            double cx = x;
            if (displayGameMode.get())
                cx = text.render(gmText, cx, y, newColor(gamemodeColor.get().getPacked(), textAlpha), shadow);
            cx = renderTeamIcon(text, cx, y, playerTeam, shadow);
            if (!name.isEmpty())
                cx = text.render(name, cx, y, newColor(nameCol.getPacked(), textAlpha), shadow);
            if (displayHealth.get())
                cx = text.render(healthText, cx, y, newColor(healthCol.getPacked(), textAlpha), shadow);
            if (displayPing.get())
                cx = text.render(pingText, cx, y, newColor(pingColor.get().getPacked(), textAlpha), shadow);
            if (displayDistance.get())
                cx = text.render(distText, cx, y, newColor(WHITE.getPacked(), textAlpha), shadow);
            if (!drText.isEmpty())
                text.render(drText, cx, y, newColor(BLACK.getPacked(), textAlpha), shadow);
            text.end();

            if (displayItems.get()) renderItemsAndEnchants(event, player, shadow, height);
            NametagUtils.end();
            return;
        }

        // 标准模式
        sb.setLength(0);
        if (displayGameMode.get()) sb.append(gmText);
        appendTeamText(sb, playerTeam);
        sb.append(name).append(healthText).append(pingText).append(distText).append(drText);
        double total = text.getWidth(sb.toString(), shadow);
        double half  = total / 2;

        Renderer2D.COLOR.begin();
        Renderer2D.COLOR.quad(-half - 1, -height - 1, total + 2, height + 2,
            newColor(background.get().getPacked(), bgAlpha));
        Renderer2D.COLOR.render();

        text.beginBig();
        double x = -half;
        if (displayGameMode.get())
            x = text.render(gmText, x, -height, newColor(gamemodeColor.get().getPacked(), textAlpha), shadow);
        x = renderTeamIcon(text, x, -height, playerTeam, shadow);
        x = text.render(name, x, -height, newColor(nameCol.getPacked(), textAlpha), shadow);
        if (displayHealth.get())
            x = text.render(healthText, x, -height, newColor(healthCol.getPacked(), textAlpha), shadow);
        if (displayPing.get())
            x = text.render(pingText, x, -height, newColor(pingColor.get().getPacked(), textAlpha), shadow);
        if (displayDistance.get())
            x = text.render(distText, x, -height, newColor(WHITE.getPacked(), textAlpha), shadow);
        if (!drText.isEmpty())
            text.render(drText, x, -height, newColor(BLACK.getPacked(), textAlpha), shadow);
        text.end();

        if (displayItems.get()) renderItemsAndEnchants(event, player, shadow, height);
        NametagUtils.end();
    }

    // ==================== 队伍图标 ====================

    private void appendTeamText(StringBuilder out, Team team) {
        if (!displayTeamIcon.get() || team == null) return;
        String prefix = team.getPrefix().getString();
        String suffix = team.getSuffix().getString();
        if (!prefix.isEmpty())      out.append(prefix);
        else if (!suffix.isEmpty()) out.append(suffix);
        else                        out.append("[T] ");
    }

    private double renderTeamIcon(TextRenderer text, double x, double y, Team team, boolean shadow) {
        if (!displayTeamIcon.get() || team == null) return x;
        String prefix = team.getPrefix().getString();
        String suffix = team.getSuffix().getString();
        String icon   = !prefix.isEmpty() ? prefix : (!suffix.isEmpty() ? suffix : "[T] ");
        Integer cv    = team.getColor().getColorValue();
        int packed    = cv != null
            ? ((cv & 0x00FFFFFF) | (textAlpha << 24))
            : ((0x00FFFFFF)      | (textAlpha << 24));
        return text.render(icon, x, y, new Color(packed), shadow);
    }

    // ==================== Items ====================

    private void renderItemsAndEnchants(Render2DEvent event, PlayerEntity player,
                                        boolean shadow, double height) {
        TextRenderer text = TextRenderer.get();
        double itemY = -height - 38;
        double itemX = -48;

        for (int i = 0; i < 6; i++) {
            ItemStack stack = getItem(player, i);
            RenderUtils.drawItem(event.drawContext, stack, (int) itemX, (int) itemY, 2, true);

            if (displayEnchants.get() && !stack.isEmpty()) {
                ItemEnchantmentsComponent ench = EnchantmentHelper.getEnchantments(stack);
                text.begin(0.9, false, true);
                double ex = itemX + 16;
                double ey = itemY;
                for (RegistryEntry<Enchantment> e : ench.getEnchantments()) {
                    sb.setLength(0);
                    sb.append(Utils.getEnchantSimpleName(e, 3)).append(" ").append(ench.getLevel(e));
                    String line = sb.toString();
                    text.render(line, ex - text.getWidth(line, shadow), ey, WHITE, shadow);
                    ey += text.getHeight(shadow);
                }
                text.end();
            }
            itemX += 18;
        }
    }

    private void renderItem(ItemStack stack, boolean shadow) {
        if (stack.isEmpty()) return;
        TextRenderer text = TextRenderer.get();
        NametagUtils.begin(pos);

        String name     = Names.get(stack);
        String countStr = (itemCount.get() && stack.getCount() > 1) ? " x" + stack.getCount() : "";

        double w = text.getWidth(name + countStr, shadow);
        double h = text.getHeight(shadow);

        Renderer2D.COLOR.begin();
        Renderer2D.COLOR.quad(-w / 2 - 1, -h - 1, w + 2, h + 2,
            newColor(background.get().getPacked(), bgAlpha));
        Renderer2D.COLOR.render();

        text.beginBig();
        text.render(name, -w / 2, -h, newColor(nameColor.get().getPacked(), textAlpha), shadow);
        if (!countStr.isEmpty())
            text.render(countStr, -w / 2 + text.getWidth(name, shadow), -h,
                newColor(WHITE.getPacked(), textAlpha), shadow);
        text.end();

        NametagUtils.end();
    }

    // ==================== Helpers ====================

    private ItemStack getItem(PlayerEntity p, int i) {
        return switch (i) {
            case 0  -> p.getMainHandStack();
            case 1  -> p.getEquippedStack(EquipmentSlot.HEAD);
            case 2  -> p.getEquippedStack(EquipmentSlot.CHEST);
            case 3  -> p.getEquippedStack(EquipmentSlot.LEGS);
            case 4  -> p.getEquippedStack(EquipmentSlot.FEET);
            case 5  -> p.getOffHandStack();
            default -> ItemStack.EMPTY;
        };
    }

    public boolean excludeBots()    { return ignoreBots.get(); }
    public boolean playerNametags() { return isActive() && entities.get().contains(EntityType.PLAYER); }

    private static float smoothstep(float edge0, float edge1, float x) {
        x = MathHelper.clamp((x - edge0) / (edge1 - edge0), 0f, 1f);
        return x * x * (3 - 2 * x);
    }

    private Color newColor(int packedRgb, int alpha) {
        return mutableColor.set(
            (packedRgb >> 16) & 0xFF,
            (packedRgb >>  8) & 0xFF,
             packedRgb        & 0xFF,
            alpha
        );
    }
}