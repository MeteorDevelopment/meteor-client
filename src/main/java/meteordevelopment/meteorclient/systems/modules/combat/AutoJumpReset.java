package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;

public class AutoJumpReset extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> chance = sgGeneral.add(new DoubleSetting.Builder()
        .name("chance")
        .description("触发概率 (100%则每次受伤都跳)")
        .defaultValue(100)
        .min(0).max(100).sliderMax(100)
        .build());

    // 很多反作弊检测你是否在根本没有击退的情况下跳跃
    private final Setting<Double> minVelocity = sgGeneral.add(new DoubleSetting.Builder()
        .name("min-velocity")
        .description("最小击退阈值 (防止被雪球/鸡蛋击中也乱跳)")
        .defaultValue(0.1)
        .min(0).max(2)
        .build());

    private final Setting<Boolean> onlyGround = sgGeneral.add(new BoolSetting.Builder()
        .name("only-ground")
        .description("仅在地面生效 (空中重置通常是作弊行为)")
        .defaultValue(true)
        .build());

    // 延迟跳跃可以让它看起来更像人类反应，绕过一些反作弊
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay-ticks")
        .description("延迟 Tick 数 (0=瞬间, 1-2=更像人类)")
        .defaultValue(1)
        .min(0).max(10)
        .build());

    public AutoJumpReset() {
        super(Categories.Combat, "auto-jump-reset", "检测到击退时自动跳跃 (防连击/减少击退)");
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null || mc.world == null) return;

        // 监听击退数据包 (Velocity)，这比受伤动画快得多！
        if (event.packet instanceof EntityVelocityUpdateS2CPacket packet) {
            // 1. 确认数据包是发给自己的
            if (packet.getEntityId() != mc.player.getId()) return;

            // 2. 概率检查
            if (Math.random() * 100 > chance.get()) return;

            // 3. 地面检查 (必须在处理击退前判断，否则击退处理完你就腾空了)
            if (onlyGround.get() && !mc.player.isOnGround()) return;

            // 4. 计算击退力度 (简单的向量长度计算)
            // Minecraft 的 Velocity 包数值很大，需要 / 8000.0 才是真实速度，但这里比较相对值就行
            double velocityX = Math.abs(packet.getVelocity().x / 8000.0);
            double velocityZ = Math.abs(packet.getVelocity().z / 8000.0);
            double totalVelocity = Math.sqrt(velocityX * velocityX + velocityZ * velocityZ);

            if (totalVelocity < minVelocity.get()) return;

            // 5. 执行跳跃
            if (delay.get() == 0) {
                mc.player.jump();
            } else {
                // 如果有延迟，我们需要在接下来的第 N 个 tick 执行
                // 这里简单使用一个标志位，或者直接利用 Meteor 的 task 系统 (如果不想太复杂，直接在这里用线程也不错，但最好用 TickEvent)
                // 既然要简单有效，我们用按键模拟，这样更兼容
                queueJump(delay.get());
            }
        }
    }

    // 一个简单的倒计时器来处理延迟跳跃
    private int jumpTimer = -1;

    @EventHandler
    private void onTick(meteordevelopment.meteorclient.events.world.TickEvent.Pre event) {
        if (jumpTimer >= 0) {
            if (jumpTimer == 0) {
                if (mc.player.isOnGround()) { // 再次确认是否在地面，防止延迟期间已经掉下悬崖
                    mc.player.jump();
                }
                jumpTimer = -1;
            } else {
                jumpTimer--;
            }
        }
    }

    private void queueJump(int ticks) {
        jumpTimer = ticks;
    }
}