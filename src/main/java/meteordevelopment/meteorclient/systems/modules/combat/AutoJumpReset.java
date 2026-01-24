package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;

public class AutoJumpReset extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // -------------------- 核心设置 --------------------

    private final Setting<Double> chance = sgGeneral.add(new DoubleSetting.Builder()
        .name("chance")
        .description("触发概率 (建议 90%-100%)")
        .defaultValue(100)
        .min(0).max(100).sliderMax(100)
        .build());

    private final Setting<Double> minVelocity = sgGeneral.add(new DoubleSetting.Builder()
        .name("min-velocity")
        .description("触发跳跃的最小击退值 (防雪球/钓鱼竿误触)")
        .defaultValue(0.15)
        .min(0).max(2)
        .build());

    private final Setting<Boolean> onlyFaceToFace = sgGeneral.add(new BoolSetting.Builder()
        .name("only-face-to-face")
        .description("仅在正面对拼时触发 (背对逃跑时不跳跃)")
        .defaultValue(true)
        .build());

    private final Setting<Double> faceAngleStrictness = sgGeneral.add(new DoubleSetting.Builder()
        .name("face-angle-strictness")
        .description("判定阈值 (-0.5=宽松, 0.0=垂直, 0.5=严格)")
        .defaultValue(0.1)
        .min(-1.0).max(1.0)
        .visible(onlyFaceToFace::get)
        .build());

    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
        .name("debug-info")
        .description("在聊天栏显示击退判定详情")
        .defaultValue(true)
        .build());

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay-ticks")
        .description("接收包后延迟多少Tick跳跃 (1=最稳, 0=最快)")
        .defaultValue(1)
        .min(0).max(5)
        .build());

    // -------------------- 内部变量 --------------------

    private int jumpTimer = -1;
    private boolean resetKeyNextTick = false;
    private boolean wasPressedByPlayer = false;

    public AutoJumpReset() {
        super(Categories.Combat, "auto-jump-reset", "智能防击退 (Grim绕过)");
    }

    @Override
    public void onDeactivate() {
        jumpTimer = -1;
        resetKeyNextTick = false;
        if (mc.options != null && resetKeyNextTick) {
            mc.options.jumpKey.setPressed(false);
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null) return;

        if (event.packet instanceof EntityVelocityUpdateS2CPacket packet) {
            if (packet.getEntityId() != mc.player.getId()) return;

            double vX = packet.getVelocity().getX();
            double vY = packet.getVelocity().getY();
            double vZ = packet.getVelocity().getZ();
            
            double velocityXZ = Math.hypot(vX, vZ);

            if (debug.get()) {
                if (velocityXZ > 0.05) {
                    ChatUtils.info("§7[Velocity] §fX:%.2f Z:%.2f (Total: %.2f)", vX, vZ, velocityXZ);
                }
            }

            // 概率检查
            if (chance.get() < 100 && Math.random() * 100 > chance.get()) return;

            // 力度检查
            if (velocityXZ < minVelocity.get()) {
                if (debug.get() && velocityXZ > 0.05) ChatUtils.info("§c[Ignored] §7Too small");
                return;
            }

            // 对拼判定 (Face-to-Face)
            if (onlyFaceToFace.get()) {
                float yaw = mc.player.getYaw();
                double pX = -Math.sin(Math.toRadians(yaw));
                double pZ = Math.cos(Math.toRadians(yaw));

                double dotProduct = vX * pX + vZ * pZ;

                if (dotProduct > faceAngleStrictness.get()) {
                    if (debug.get()) ChatUtils.info("§c[Ignored] §7Not facing (Dot: %.2f)", dotProduct);
                    return;
                }
                
                if (debug.get()) ChatUtils.info("§a[Trigger] §7Face-to-Face (Dot: %.2f)", dotProduct);
            }

            if (mc.player.isOnGround() || !mc.player.getAbilities().flying) {
                jumpTimer = delay.get();
                if (debug.get()) ChatUtils.info("§b[Jump] §fScheduled in " + delay.get() + " ticks");
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (jumpTimer > 0) {
            jumpTimer--;
        } else if (jumpTimer == 0) {
            performJump();
            jumpTimer = -1;
        }
    }
    
    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        if (resetKeyNextTick) {
            if (!wasPressedByPlayer) {
                mc.options.jumpKey.setPressed(false);
            }
            resetKeyNextTick = false;
        }
    }

    private void performJump() {
        if (mc.player == null) return;

        if (mc.player.isOnGround()) {
            wasPressedByPlayer = mc.options.jumpKey.isPressed();
            mc.options.jumpKey.setPressed(true);
            resetKeyNextTick = true;
        }
    }
}