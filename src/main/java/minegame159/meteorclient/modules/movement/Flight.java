package minegame159.meteorclient.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.*;

public class Flight extends ToggleModule {
    public enum Mode {
        Vanilla
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Mode.")
            .defaultValue(Mode.Vanilla)
            .build()
    );

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed")
            .description("Speed.")
            .defaultValue(0.1)
            .min(0.0)
            .build()
    );

    private final Setting<Boolean> antiKick = sgGeneral.add(new BoolSetting.Builder()
            .name("anti-kick"
            ).description("Toggles flight to try and stop you getting kicked.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("The time in between toggles.(20 ticks = 1 second)")
            .defaultValue(60)
            .min(1)
            .max(5000)
            .sliderMax(200)
            .build()
    );

    private final Setting<Integer> offTime = sgGeneral.add(new IntSetting.Builder()
            .name("off-time")
            .description("The time the flight is toggled off.(20 ticks = 1 second)")
            .defaultValue(5)
            .min(1)
            .max(20)
            .sliderMax(10)
            .build()
    );

    public Flight() {
        super(Category.Movement, "flight", "FLYYYY! You will take fall damage so enable no fall.");
    }

    private int delayLeft = delay.get();
    private int offLeft = offTime.get();

    @Override
    public void onActivate() {
        if (mode.get() == Mode.Vanilla && !mc.player.isSpectator()) {
            mc.player.abilities.flying = true;
            if (mc.player.abilities.creativeMode) return;
            mc.player.abilities.allowFlying = true;
        }
    }

    @Override
    public void onDeactivate() {
        if (mode.get() == Mode.Vanilla && !mc.player.isSpectator()) {
            mc.player.abilities.flying = false;
            mc.player.abilities.setFlySpeed(0.05f);
            if (mc.player.abilities.creativeMode) return;
            mc.player.abilities.allowFlying = false;
        }
    }

    @EventHandler
    private final Listener<TickEvent> onTick = new Listener<>(event -> {
        if (antiKick.get() && delayLeft > 0) {
            delayLeft --;
        } else if (antiKick.get() && delayLeft <= 0 && offLeft > 0) {
            offLeft --;
            mc.player.abilities.flying = false;
            mc.player.abilities.setFlySpeed(0.05f);
            if (mc.player.abilities.creativeMode) return;
            mc.player.abilities.allowFlying = false;
            return;
        }else if (antiKick.get() && delayLeft <=0 && offLeft <= 0) {
            delayLeft = delay.get();
            offLeft = offTime.get();
        }
        if (mode.get() == Mode.Vanilla && !mc.player.isSpectator()) {
            mc.player.abilities.setFlySpeed(speed.get().floatValue());
            mc.player.abilities.flying = true;
            if (mc.player.abilities.creativeMode) return;
            mc.player.abilities.allowFlying = true;
        }
    });
}
