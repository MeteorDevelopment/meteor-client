/*
 * 1.21.10 终极硬核 Sprint - 水陆全冲 + 惯性可调 + tap W 丝滑到底
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class Sprint2 extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> keepSprint = sgGeneral.add(new BoolSetting.Builder()
        .name("keep-sprint").description("攻击后保持冲刺").defaultValue(false).build());

    private final Setting<Boolean> unsprintOnHit = sgGeneral.add(new BoolSetting.Builder()
        .name("unsprint-on-hit").description("攻击前停冲保证暴击").defaultValue(false).build());

    public enum PreAttackAction { None, Unsprint, ReleaseForward, HoldBack }
    private final Setting<PreAttackAction> preAttackAction = sgGeneral.add(new EnumSetting.Builder<PreAttackAction>()
        .name("pre-attack-action").defaultValue(PreAttackAction.ReleaseForward).build());

    private final Setting<Integer> preAttackDuration = sgGeneral.add(new IntSetting.Builder()
        .name("pre-attack-duration").defaultValue(2).min(1).max(10).build());
    private final Setting<Integer> restoreSprintDelay = sgGeneral.add(new IntSetting.Builder()
        .name("restore-sprint-delay").defaultValue(5).min(0).max(10).build());

    // 新增：惯性 tick 可调节（默认 4）
    private final Setting<Integer> sprintInertiaTicks = sgGeneral.add(new IntSetting.Builder()
        .name("sprint-inertia")
        .description("松开方向键后继续保持冲刺的 tick 数（防 tap W 闪烁）")
        .defaultValue(4)
        .min(1)
        .max(20)
        .sliderMax(20)
        .build());

    // 必暴储备（按 R）
    private final Setting<Boolean> autoResetCritReserve = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-reset-crit-reserve").defaultValue(true).build());
    private final Setting<Integer> critReserveTimeout = sgGeneral.add(new IntSetting.Builder()
        .name("crit-reserve-timeout").defaultValue(60).min(1).max(200).build());

    public Sprint2() {
        super(Categories.Movement, "sprint", "水陆全冲 · 惯性可调 · 终极硬核自动冲刺");
    }

    private int suppressTimer = 0;
    private int forwardTimer = 0, backTimer = 0, restoreTimer = 0;
    private boolean origF = false, origB = false;
    private int sprintInertia = 0;     // 当前剩余惯性 tick
    private boolean lastHadInput = false;
    private boolean wasRPressed = false;

    public static int critReserve = 0;
    private static int critIdleTicks = 0;

    @Override public void onActivate() { mc.options.sprintKey.setPressed(false); }

    @EventHandler(priority = EventPriority.HIGH)
    private void onTick(TickEvent.Post event) {
        if (suppressTimer > 0) {
            suppressTimer--;
            sprintInertia = 0;
            return;
        }

        boolean desired = shouldSprintOriginal();

        Vec2f input = mc.player.input.getMovementInput();
        boolean hasInput = Math.abs(input.y) > 0.01f || Math.abs(input.x) > 0.01f;

        // 有输入 → 刷新惯性
        if (hasInput && !lastHadInput) {
            sprintInertia = sprintInertiaTicks.get();
        }
        if (sprintInertia > 0) sprintInertia--;

        boolean should = desired || (sprintInertia > 0);

        mc.player.setSprinting(should);
        lastHadInput = hasInput;
        mc.options.sprintKey.setPressed(false);
    }

    private boolean shouldSprintOriginal() {
        if (!isActive()) return false;
        if (mc.currentScreen != null && !Modules.get().get(GUIMove.class).sprint.get()) return false;

        float f = Math.abs(mc.player.forwardSpeed);
        float s = Math.abs(mc.player.sidewaysSpeed);

        boolean movingForwardEnough = f > 0.8f;
        boolean movingAny = (f + s) > 0.1f;

        if (movingForwardEnough) return true;
        if (mc.player.isSprinting() && movingAny) return true;
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onSendPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof PlayerInteractEntityC2SPacket p)) return;
        p.handle(new PlayerInteractEntityC2SPacket.Handler() {
            @Override public void interact(Hand h) {}
            @Override public void interactAt(Hand h, Vec3d pos) {}
            @Override public void attack() {
                PreAttackAction a = unsprintOnHit.get() ? PreAttackAction.Unsprint : preAttackAction.get();
                doPreAttack(a);
            }
        });
    }

    private void doPreAttack(PreAttackAction a) {
        switch (a) {
            case Unsprint -> {
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                mc.player.setSprinting(false);
                suppressTimer = restoreSprintDelay.get();
            }
            case ReleaseForward -> {
                origF = mc.options.forwardKey.isPressed();
                mc.options.forwardKey.setPressed(false);
                forwardTimer = preAttackDuration.get();
                suppressTimer = restoreSprintDelay.get();
            }
            case HoldBack -> {
                origB = mc.options.backKey.isPressed();
                mc.options.backKey.setPressed(true);
                backTimer = preAttackDuration.get();
                suppressTimer = restoreSprintDelay.get();
            }
        }
    }

    @EventHandler
    private void onTickRestore(TickEvent.Post event) {
        if (forwardTimer > 0 && --forwardTimer == 0) mc.options.forwardKey.setPressed(origF);
        if (backTimer > 0 && --backTimer == 0) mc.options.backKey.setPressed(origB);
        if (restoreTimer > 0 && --restoreTimer == 0) {
            if (shouldSprintOriginal() && !mc.player.isSprinting()) {
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
                mc.player.setSprinting(true);
            }
        }
    }

    @EventHandler
    private void onTickCrit(TickEvent.Post event) {
        boolean r = GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS;
        if (r && !wasRPressed && critReserve < 2) {
            critReserve++;
            critIdleTicks = 0;
            ChatUtils.info("必暴击剩余: " + critReserve);
        }
        if (critReserve > 0) {
            critIdleTicks++;
            if (autoResetCritReserve.get() && critIdleTicks >= critReserveTimeout.get()) {
                critReserve = 0;
                critIdleTicks = 0;
                ChatUtils.info("必暴击已超时清零");
            }
        }
        wasRPressed = r;
    }

    public static void notifyCritConsumed() {
        critIdleTicks = 0;
    }

    // 必须的三个方法（防崩溃）
    public boolean rageSprint() { return false; }
    public boolean unsprintInWater() { return false; }  // 水里永远冲！
    public boolean stopSprinting() { return !isActive() || !keepSprint.get(); }
}