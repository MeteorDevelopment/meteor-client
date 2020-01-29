package minegame159.meteorclient.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.mixininterface.IKeyBinding;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.builders.EnumSettingBuilder;

public class AutoSprint extends Module {
    public enum Mode {
        Always,
        Legit
    }

    private Setting<Mode> mode = addSetting(new EnumSettingBuilder<Mode>()
            .name("mode")
            .description("Mode.")
            .defaultValue(Mode.Always)
            .build()
    );

    public AutoSprint() {
        super(Category.Movement, "auto-sprint", "Automatically sprints.");
    }

    @Override
    public void onDeactivate() {
        setSprinting(false);
    }

    private void setSprinting(boolean sprinting) {
        if (mode.value() == Mode.Always) mc.player.setSprinting(sprinting);
        else ((IKeyBinding) mc.options.keySprint).setPressed(sprinting);
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        if (mc.player.forwardSpeed > 0 && !mc.player.horizontalCollision && !mc.player.isSneaking()) setSprinting(true);
        else setSprinting(false);
    });
}
