package minegame159.meteorclient.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.builders.EnumSettingBuilder;

public class AutoJump extends Module {
    public enum JumpIf {
        Sprinting,
        Walking,
        Always
    }

    private Setting<JumpIf> jumpIf = addSetting(new EnumSettingBuilder<JumpIf>()
            .name("jump-if")
            .description("Jump if.")
            .defaultValue(JumpIf.Always)
            .build()
    );

    public AutoJump() {
        super(Category.Movement, "auto-jump", "Automatically jumps.");
    }

    private boolean jump() {
        switch (jumpIf.value()) {
            case Sprinting: return mc.player.isSprinting() && (mc.player.forwardSpeed != 0 || mc.player.sidewaysSpeed != 0);
            case Walking:   return mc.player.forwardSpeed != 0 || mc.player.sidewaysSpeed != 0;
            case Always:    return true;
            default:        return false;
        }
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        if (!mc.player.onGround || mc.player.isSneaking()) return;

        if (jump()) mc.player.jump();
    });
}
