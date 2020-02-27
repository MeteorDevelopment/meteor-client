package minegame159.meteorclient.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.builders.DoubleSettingBuilder;
import minegame159.meteorclient.settings.builders.EnumSettingBuilder;

public class Flight extends Module {
    public enum Mode {
        Vanilla
    }

    private Setting<Mode> mode = addSetting(new EnumSettingBuilder<Mode>()
            .name("mode")
            .description("Mode.")
            .defaultValue(Mode.Vanilla)
            .build()
    );

    private Setting<Double> speed = addSetting(new DoubleSettingBuilder()
            .name("speed")
            .description("Speed.")
            .defaultValue(0.1)
            .min(0.0)
            .build()
    );

    public Flight() {
        super(Category.Movement, "flight", "FLYYYY! You will take fall damage so enable no fall.");
    }

    @Override
    public void onActivate() {
        if (mode.value() == Mode.Vanilla) {
            mc.player.abilities.flying = true;
            if (mc.player.abilities.creativeMode) return;
            mc.player.abilities.allowFlying = true;
        }
    }

    @Override
    public void onDeactivate() {
        if (mode.value() == Mode.Vanilla) {
            mc.player.abilities.flying = false;
            mc.player.abilities.setFlySpeed(0.05f);
            if (mc.player.abilities.creativeMode) return;
            mc.player.abilities.allowFlying = false;
        }
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        switch (mode.value()) {
            case Vanilla:
                mc.player.abilities.setFlySpeed(speed.value().floatValue());
                mc.player.abilities.flying = true;
                if (mc.player.abilities.creativeMode) return;
                mc.player.abilities.allowFlying = true;
                break;
        }
    });
}
