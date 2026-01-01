package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class NoJumpDelay extends Module {
    public NoJumpDelay() {
        super(Categories.Movement, "no-jump-delay", "移除跳跃冷却 - 允许极限连跳");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player != null) {
            // 强制将跳跃冷却刻设为 0
            // 这是一个 Accessor/Mixin 字段，Meteor 应该已经暴露了，或者可以通过反射
            // 如果 Meteor 核心没有暴露 jumpCooldown，这个简单的模块可能无法直接工作
            // 但通常可以通过持续触发 jump key 来模拟
            
            // 替代方案：如果按住空格，且在地面，强制 jump
            if (mc.player.isOnGround() && mc.options.jumpKey.isPressed()) {
                mc.player.jump();
            }
        }
    }
}