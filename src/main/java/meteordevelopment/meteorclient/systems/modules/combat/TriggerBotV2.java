package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.systems.modules.movement.Sprint;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.*;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;
import java.util.Set;

public class TriggerBotV2 extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgReach = settings.createGroup("Reach");
    private final SettingGroup sgTiming = settings.createGroup("Timing");
    private final SettingGroup sgCurve = settings.createGroup("第二刀曲线");
    private final SettingGroup sgLag = settings.createGroup("延迟补偿");
    private final SettingGroup sgDebug = settings.createGroup("Debug");

    private final Random random = new Random();
    private boolean isFirstAttack = true;
    private double lastThreshold = 0.95; // 初始值跟随首刀

    public TriggerBotV2() {
        super(Categories.Combat, "trigger-bot++", "2025终极纯攻击版 • Ping补偿优化");
    }

    // ==================== General ====================
    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("entities").description("可攻击实体").onlyAttackable()
        .defaultValue(Set.of(EntityType.PLAYER)).build());

    private final Setting<Boolean> babies = sgGeneral.add(new BoolSetting.Builder()
        .name("babies").description("打幼年动物").defaultValue(true).build());

    private final Setting<Boolean> fastGrassBreak = sgGeneral.add(new BoolSetting.Builder()
        .name("fast-grass-break").description("快速打草功能").defaultValue(true).build());

    // ==================== Reach ====================
    private final Setting<Double> maxExtraReach = sgReach.add(new DoubleSetting.Builder()
        .name("max-extra-reach")
        .description("额外Reach上限（0.01最稳，低打高完美）")
        .defaultValue(0.01).min(-0.5).max(0.50).sliderMin(-0.5).sliderMax(0.50).build());

    // ==================== Timing (已修改：首刀95%) ====================
    private final Setting<Double> firstAttackThreshold = sgTiming.add(new DoubleSetting.Builder()
        .name("first-threshold")
        .description("首刀阈值")
        .defaultValue(0.95) // 默认值调整为0.95
        .min(0.75).max(1.0) // 最小值调整为0.75
        .sliderMax(1.0)
        .build());

    // ==================== Curve (已修改：后续 93% - 100%) ====================
    private final Setting<Double> minThreshold = sgCurve.add(new DoubleSetting.Builder()
        .name("min-threshold")
        .defaultValue(0.93) // 修改：下限 0.93
        .min(0.75).max(1.0) // 调整范围为0.75-1.0
        .build());

    private final Setting<Double> splitPoint = sgCurve.add(new DoubleSetting.Builder()
        .name("split-point")
        .defaultValue(0.96) // 修改：中间点，确保在 0.93-1.0 之间
        .min(0.75).max(1.0) // 调整范围为0.75-1.0
        .build());

    private final Setting<Double> maxThreshold = sgCurve.add(new DoubleSetting.Builder()
        .name("max-threshold")
        .defaultValue(1.0) // 修改：上限 1.0
        .min(0.75).max(1.0) // 调整范围为0.75-1.0
        .build());

    private final Setting<Double> mainProbability = sgCurve.add(new DoubleSetting.Builder()
        .name("main-probability-%").defaultValue(75.0).min(50.0).max(95.0).build());
    private final Setting<Double> compressPower = sgCurve.add(new DoubleSetting.Builder()
        .name("compress-power").defaultValue(0.38).min(0.1).max(1.0).build());
    private final Setting<Double> tailPower = sgCurve.add(new DoubleSetting.Builder()
        .name("tail-power").defaultValue(2.4).min(1.0).max(4.0).build());
    private final Setting<Double> handShakeChance = sgCurve.add(new DoubleSetting.Builder()
        .name("handshake-chance-%").defaultValue(1.8).min(0.0).max(10.0).build());
    private final Setting<Double> handShakeExtra = sgCurve.add(new DoubleSetting.Builder()
        .name("handshake-extra").defaultValue(0.02).min(0.01).max(0.10).build()); // 调小抖动，防止超出84%太多

    // ==================== Ping Compensation ====================
    private final Setting<Boolean> pingCompensate = sgLag.add(new BoolSetting.Builder()
        .name("ping-compensate")
        .description("根据延迟微调阈值，防止高延迟下吞刀")
        .defaultValue(true)
        .build());

    private final Setting<Double> pingFactor = sgLag.add(new DoubleSetting.Builder()
        .name("ping-factor")
        .description("补偿系数 (建议 0.5 - 1.0)")
        .defaultValue(0.5).min(0.1).max(2.0)
        .visible(pingCompensate::get)
        .build());

    private final Setting<Boolean> hitDebug = sgDebug.add(new BoolSetting.Builder()
        .name("hit-debug").description("出刀日志").defaultValue(true).build());

    @Override
    public void onActivate() {
        isFirstAttack = true;
        lastThreshold = firstAttackThreshold.get();
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        float cooldown = mc.player.getAttackCooldownProgress(0.0f);
        Entity target = getTarget();

        if (target != null) {
            double extra = random.nextDouble() * maxExtraReach.get();
            double maxReach = 3.0 + extra;
            
            Vec3d eye = mc.player.getEyePos();
            Vec3d hitPos;
            
            if (mc.crosshairTarget instanceof EntityHitResult ehr && ehr.getEntity() == target) {
                 hitPos = ehr.getPos();
            } else {
                 hitPos = target.getBoundingBox().getCenter(); 
            }
            
            double hitDist = eye.distanceTo(hitPos);
            boolean inRange = hitDist <= maxReach + 0.1;

            if (inRange && canAttackNow(cooldown)) {
                attack(target, hitDist, extra);
                isFirstAttack = false;
            }
        }
    }

    private Entity getTarget() {
        if (mc.crosshairTarget == null) return null;

        if (mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            if (fastGrassBreak.get() && mc.crosshairTarget instanceof BlockHitResult bhr && isBreakableGrassBlock(bhr.getBlockPos())) {
                mc.interactionManager.attackBlock(bhr.getBlockPos(), bhr.getSide());
                mc.player.swingHand(Hand.MAIN_HAND);
            }
            return null;
        }

        if (mc.crosshairTarget instanceof EntityHitResult ehr) {
            Entity target = ehr.getEntity();
            if (isValid(target)) return target;
        }

        return null;
    }

    private boolean isBreakableGrassBlock(net.minecraft.util.math.BlockPos pos) {
        if (mc.world == null) return false;
        var state = mc.world.getBlockState(pos);
        if (state.isAir()) return false;
        return state.getBlock().getHardness() <= 0.5;
    }

    private boolean canAttackNow(float progress) {
        double threshold;

        if (isFirstAttack) {
            // 首刀 95%，加微小随机 (-0.005 ~ +0.005)，避免完全固定被检测
            threshold = firstAttackThreshold.get() + (random.nextDouble() - 0.5) * 0.01;
        } else {
            // 连刀逻辑：在 0.79 (min) 到 0.84 (max) 之间波动
            double r = random.nextDouble();
            double main = mainProbability.get() / 100.0;
            
            // 确保 splitPoint 不会超出 min/max 范围导致计算错误
            double safeMin = minThreshold.get();
            double safeMax = maxThreshold.get();
            double safeSplit = MathHelper.clamp(splitPoint.get(), safeMin, safeMax);

            if (r < main) {
                // 前段曲线 (min ~ split)
                threshold = safeMin + Math.pow(random.nextDouble(), compressPower.get()) * (safeSplit - safeMin);
            } else {
                // 后段曲线 (split ~ max)
                threshold = safeSplit + Math.pow(random.nextDouble(), tailPower.get()) * (safeMax - safeSplit);
            }

            // 偶尔的“手抖”增加一点点阈值，模拟真人
            if (random.nextDouble() * 100 < handShakeChance.get()) {
                threshold += random.nextDouble() * handShakeExtra.get();
            }
        }

        // ==================== Ping 补偿 ====================
        // 如果开启补偿，在高延迟下会适当降低阈值（提前发包），
        // 这样服务器收到时正好是设置的阈值。
        if (pingCompensate.get() && mc.getNetworkHandler() != null) {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) {
                int latency = entry.getLatency();
                double offset = (latency / 1000.0) * pingFactor.get();
                threshold -= offset;
            }
        }

        // 最终限制在 0.6 ~ 1.0 之间
        lastThreshold = MathHelper.clamp(threshold, 0.6, 1.0);
        return progress >= lastThreshold;
    }

    private void attack(Entity target, double hitDist, double extra) {
        if (target == null || mc.player == null) return;

        boolean critCondition = canCrit();

        if (Sprint.instance != null && Sprint.instance.isActive() && critCondition && mc.player.isSprinting()) {
             Sprint.requestCritUnsprint(() -> performAttack(target, hitDist, extra, true));
        } else {
             performAttack(target, hitDist, extra, critCondition);
        }
    }

    private void performAttack(Entity target, double hitDist, double extra, boolean isCrit) {
        if (mc.player == null) return;

        if (hitDebug.get()) {
            String type = isFirstAttack ? "§a首刀" : "§b连刀";
            String critStr = isCrit ? "§c暴击" : "§8平击";
            float cooldownPct = mc.player.getAttackCooldownProgress(0.0f) * 100;
            
            int ping = 0;
            if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) != null) {
                ping = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency();
            }

            ChatUtils.info("§b§lTB++ §r%s §8| §f%s §8| §e%.3f §8| §6%s §8| §7CD: %.1f%% §8| §9Ping: %dms",
                type, target.getName().getString(), hitDist, critStr, cooldownPct, ping);
        }

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private boolean canCrit() {
        return mc.player != null
            && mc.player.fallDistance > 0.0F
            && !mc.player.isOnGround()
            && !mc.player.isClimbing()
            && !mc.player.isTouchingWater()
            && !mc.player.isInLava()
            && !mc.player.hasVehicle();
    }

    private boolean isValid(Entity e) {
        if (e == null || e == mc.player || !e.isAlive()) return false;
        if (e instanceof LivingEntity le && le.isDead()) return false;
        if (e instanceof PlayerEntity p) {
            if (p.isCreative() || p.isSpectator()) return false;
            if (!Friends.get().shouldAttack(p)) return false;
        }
        if (!babies.get() && e instanceof AnimalEntity a && a.isBaby()) return false;
        return entities.get().contains(e.getType());
    }
}