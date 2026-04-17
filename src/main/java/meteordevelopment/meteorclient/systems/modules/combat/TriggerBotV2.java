package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.MinecraftClientAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.movement.UltimateSprint;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;

import java.util.Random;
import java.util.Set;

public class TriggerBotV2 extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgLogic   = settings.createGroup("Logic (判定)");
    private final SettingGroup sgTiming  = settings.createGroup("Timing (延迟/冷却)");

    private final Random random = new Random();

    // ==================== General ====================
    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("entities").defaultValue(Set.of(EntityType.PLAYER)).build());

    private final Setting<Boolean> checkTeams = sgGeneral.add(new BoolSetting.Builder()
        .name("check-teams").defaultValue(true).build());

    // ==================== Logic ====================
    private final Setting<Boolean> smartAirSwing = sgLogic.add(new BoolSetting.Builder()
        .name("smart-air-swing").defaultValue(true).build());

    private final Setting<Double> airSwingMin = sgLogic.add(new DoubleSetting.Builder()
        .name("air-swing-min").defaultValue(4.5).visible(smartAirSwing::get).build());

    private final Setting<Double> airSwingMax = sgLogic.add(new DoubleSetting.Builder()
        .name("air-swing-max").defaultValue(5.5).visible(smartAirSwing::get).build());

    // ==================== Timing ====================
    private final Setting<Double> critThreshold = sgTiming.add(new DoubleSetting.Builder()
        .name("crit-threshold").defaultValue(0.72).build());

    private final Setting<Double> firstHitThreshold = sgTiming.add(new DoubleSetting.Builder()
        .name("first-hit-threshold").defaultValue(0.80).build());

    private final Setting<Double> comboMinThreshold = sgTiming.add(new DoubleSetting.Builder()
        .name("combo-min").defaultValue(0.875).build());

    private final Setting<Double> comboMaxThreshold = sgTiming.add(new DoubleSetting.Builder()
        .name("combo-max").defaultValue(1.0).build());

    // ==================== State (Static) ====================
    private static Entity lockedTarget = null;

    // ==================== State (Instance) ====================
    private boolean isFirstAttack        = true;
    private Entity  lastTickTarget       = null;
    private double  cachedComboThreshold = -1;
    private boolean hasClickedThisTick   = false;

    public TriggerBotV2() {
        super(Categories.Combat, "trigger-bot-v2", "Instant activation. (0-Delay when walking)");
    }

    @Override
    public void onActivate() {
        resetState();
    }

    @Override
    public void onDeactivate() {
        resetState();
    }

    private void resetState() {
        isFirstAttack        = true;
        lockedTarget         = null;
        lastTickTarget       = null;
        cachedComboThreshold = -1;
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onPreTick(TickEvent.Pre event) {
        hasClickedThisTick = false;
        if (mc.player == null || mc.world == null || mc.player.isUsingItem()) return;

        // --- Layer 1: 重锤快速路径 ---
        if (isMace()) {
            if (mc.player.fallDistance >= 1.5f) {
                Entity target = getCrosshairTarget();
                if (target != null) {
                    doLeftClick(false);
                }
            }
            return;
        }

        // --- Layer 2: Smart Air Swing ---
        if (isFirstAttack && smartAirSwing.get()) {
            Entity crosshairTarget = getCrosshairTarget();
            boolean targetIsPlayer = crosshairTarget instanceof PlayerEntity;

            // 只有准星对着玩家、或没对着任何合法目标时，才执行空挥逻辑
            if (targetIsPlayer || crosshairTarget == null) {
                double range = airSwingMin.get() + (random.nextDouble() * (airSwingMax.get() - airSwingMin.get()));
                if (getNearbyPlayersCount(range) == 0) {
                    doLeftClick(false);
                    return;
                }
            }
        }

        // --- Layer 3: Combat Logic ---
        Entity currentTarget = getCrosshairTarget();

        // 目标死亡时重置锁定
        if (lockedTarget != null && !lockedTarget.isAlive()) {
            lockedTarget         = null;
            isFirstAttack        = true;
            cachedComboThreshold = -1;
        }

        if (currentTarget == null) {
            lastTickTarget = null;
            return;
        }

        // 连击时，只有当准星指向已锁定的目标才处理
        if (!isFirstAttack && currentTarget != lockedTarget) {
            lastTickTarget = currentTarget;
            return;
        }

        lastTickTarget = currentTarget;

        boolean shouldCrit = canCrit();

        if (!isFirstAttack && cachedComboThreshold == -1) {
            cachedComboThreshold = comboMinThreshold.get()
                + (random.nextDouble() * (comboMaxThreshold.get() - comboMinThreshold.get()));
        }

        float progress = mc.player.getAttackCooldownProgress(0.5f);
        float predictedProgress = progress;

        boolean readyToAttack = false;

        if (shouldCrit) {
            readyToAttack = (predictedProgress >= critThreshold.get());
        } else {
            if (isFirstAttack) {
                readyToAttack = (predictedProgress >= firstHitThreshold.get());
            } else {
                readyToAttack = (predictedProgress >= cachedComboThreshold);
            }
        }

        if (readyToAttack) {
            if (isFirstAttack) lockedTarget = currentTarget;
            doLeftClick(shouldCrit);
            postAttackProcessing();
        }
    }

    private void postAttackProcessing() {
        isFirstAttack        = false;
        cachedComboThreshold = -1;
    }

    private void doLeftClick(boolean requestCrit) {
        if (hasClickedThisTick) return;
        if (requestCrit) {
            UltimateSprint.requestCritUnsprint(() -> ((MinecraftClientAccessor) mc).meteor$leftClick());
        } else {
            ((MinecraftClientAccessor) mc).meteor$leftClick();
        }
        hasClickedThisTick = true;
    }

    private Entity getCrosshairTarget() {
        if (mc.crosshairTarget instanceof EntityHitResult ehr) {
            Entity entity = ehr.getEntity();
            if (isValid(entity)) return entity;
        }
        return null;
    }

    private boolean isValid(Entity e) {
        if (e == null || !e.isAlive() || e == mc.player) return false;
        if (e instanceof LivingEntity le && le.getHealth() <= 0) return false;
        if (!entities.get().contains(e.getType())) return false;
        if (e instanceof PlayerEntity p) {
            if (p.isCreative() || p.isSpectator()) return false;
            if (!Friends.get().shouldAttack(p)) return false;
            if (checkTeams.get() && isTeammate(p)) return false;
        }
        return !(e instanceof AnimalEntity a) || !a.isBaby();
    }

    private boolean isTeammate(PlayerEntity p) {
        if (mc.player.isTeammate(p)) return true;
        AbstractTeam myTeam     = mc.player.getScoreboardTeam();
        AbstractTeam targetTeam = p.getScoreboardTeam();
        return myTeam != null && targetTeam != null && myTeam.getColor() == targetTeam.getColor();
    }

    private int getNearbyPlayersCount(double range) {
        if (mc.world == null) return 0;
        Box box = mc.player.getBoundingBox().expand(range);
        return mc.world.getOtherEntities(mc.player, box, e ->
            e instanceof PlayerEntity p && isValid(p)
        ).size();
    }

    private boolean isMace() {
        return mc.player.getMainHandStack().isOf(Items.MACE);
    }

    private boolean canCrit() {
        return !mc.player.isOnGround()
            && mc.player.fallDistance > 0.0f
            && !mc.player.isClimbing()
            && !mc.player.isSubmergedInWater()
            && !mc.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.BLINDNESS)
            && mc.player.getVehicle() == null;
    }
}